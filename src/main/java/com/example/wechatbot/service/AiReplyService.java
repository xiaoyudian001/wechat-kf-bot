package com.example.wechatbot.service;

import com.example.wechatbot.config.AiProperties;
import com.example.wechatbot.model.OpenAiChatRequest;
import com.example.wechatbot.model.OpenAiChatResponse;
import com.example.wechatbot.model.ReplyDecision;
import com.example.wechatbot.model.ReplyRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * 描述：负责根据规则、FAQ 和大模型生成最终客服回复。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class AiReplyService {

    private static final Set<String> SUPPORTED_MESSAGE_ROLES = Set.of("system", "user", "assistant");
    private static final Pattern THINK_BLOCK_PATTERN = Pattern.compile("(?is)<think>.*?</think>\\s*");

    private final AiProperties aiProperties;
    private final KnowledgeService knowledgeService;
    private final SensitiveRuleService sensitiveRuleService;
    private final ConversationMemoryService conversationMemoryService;
    private final OrderIntentService orderIntentService;
    private final FaqMatchService faqMatchService;
    private final RestTemplate restTemplate;

    /**
     * 描述：根据用户文本生成回复决策。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public ReplyDecision generateReply(String externalUserId, String userText) {
        if (sensitiveRuleService.shouldHandoff(userText)) {
            return new ReplyDecision(ReplyRoute.SENSITIVE_HANDOFF, sensitiveRuleService.handoffReply());
        }

        if (orderIntentService.isOrderRelated(userText)) {
            if (!orderIntentService.hasEnoughOrderInfo(userText)) {
                return new ReplyDecision(ReplyRoute.ORDER_INFO_REQUIRED, orderIntentService.askForOrderInfo());
            }
            return new ReplyDecision(ReplyRoute.ORDER_PENDING, "收到，我这边先帮您核实订单信息，请稍等一下。");
        }

        Optional<String> faqAnswer = faqMatchService.match(userText);
        if (faqAnswer.isPresent()) {
            return new ReplyDecision(ReplyRoute.FAQ_MATCH, faqAnswer.get());
        }

        OpenAiChatRequest request = new OpenAiChatRequest();
        request.setModel(aiProperties.getModel());
        request.setMessages(buildMessages(externalUserId, userText));
        log.debug("ai_reply_request baseUrl={}, model={}, minimalTestMode={}, messageCount={}",
                aiProperties.getBaseUrl(),
                aiProperties.getModel(),
                aiProperties.isMinimalTestMode(),
                request.getMessages().size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(StringUtils.trimToEmpty(aiProperties.getApiKey()));

        HttpEntity<OpenAiChatRequest> entity = new HttpEntity<>(request, headers);
        OpenAiChatResponse response;
        try {
            response = restTemplate.postForObject(
                    aiProperties.getBaseUrl(),
                    entity,
                    OpenAiChatResponse.class
            );
        } catch (RestClientResponseException e) {
            log.error("ai_reply_http_failed baseUrl={}, model={}, apiKey={}, status={}, responseBody={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel(),
                    maskApiKey(aiProperties.getApiKey()),
                    e.getStatusCode(),
                    abbreviate(e.getResponseBodyAsString()),
                    e);
            return new ReplyDecision(ReplyRoute.FALLBACK_HANDOFF, sensitiveRuleService.handoffReply());
        } catch (UnknownContentTypeException e) {
            log.error("ai_reply_unexpected_content_type baseUrl={}, model={}, apiKey={}, contentType={}, responseBody={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel(),
                    maskApiKey(aiProperties.getApiKey()),
                    e.getContentType(),
                    abbreviate(e.getResponseBodyAsString()),
                    e);
            return new ReplyDecision(ReplyRoute.FALLBACK_HANDOFF, sensitiveRuleService.handoffReply());
        } catch (RestClientException e) {
            log.error("ai_reply_request_failed baseUrl={}, model={}, apiKey={}, error={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel(),
                    maskApiKey(aiProperties.getApiKey()),
                    e.getMessage(),
                    e);
            return new ReplyDecision(ReplyRoute.FALLBACK_HANDOFF, sensitiveRuleService.handoffReply());
        }

        if (response == null
                || response.getChoices() == null
                || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null
                || StringUtils.isBlank(response.getChoices().get(0).getMessage().getContent())) {
            log.error("ai_reply_empty_response baseUrl={}, model={}, response={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel(),
                    response);
            return new ReplyDecision(ReplyRoute.FALLBACK_HANDOFF, sensitiveRuleService.handoffReply());
        }

        String reply = cleanModelReply(response.getChoices().get(0).getMessage().getContent());
        if (StringUtils.isBlank(reply)) {
            log.error("ai_reply_blank_after_cleaning baseUrl={}, model={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel());
            return new ReplyDecision(ReplyRoute.FALLBACK_HANDOFF, sensitiveRuleService.handoffReply());
        }
        return new ReplyDecision(ReplyRoute.AI_REPLY, reply);
    }

    private List<OpenAiChatRequest.Message> buildMessages(String externalUserId, String userText) {
        List<OpenAiChatRequest.Message> messages = new ArrayList<>();
        if (aiProperties.isMinimalTestMode()) {
            addMessage(messages, "user", userText);
            return messages;
        }

        addMessage(messages, "system", buildSystemMessage());
        for (ConversationMemoryService.ChatMessage msg : conversationMemoryService.getRecentMessages(externalUserId)) {
            normalizeMessageRole(msg.role())
                    .ifPresent(role -> addMessage(messages, role, msg.content()));
        }
        addMessage(messages, "user", userText);
        return messages;
    }

    private String buildSystemMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.defaultString(aiProperties.getSystemPrompt()).trim());
        builder.append("\n\n不要输出思考过程，不要输出 <think> 标签，只输出最终给客户看的回复。");

        String faqContent = StringUtils.trimToEmpty(knowledgeService.getFaqContent());
        if (StringUtils.isNotBlank(faqContent)) {
            builder.append("\n\n以下是民宿知识库，请优先依据这些信息回答。如果知识库没有明确说明，不要编造。\n\n");
            builder.append(faqContent);
        }
        return builder.toString();
    }

    private void addMessage(List<OpenAiChatRequest.Message> messages, String role, String content) {
        if (StringUtils.isBlank(content)) {
            return;
        }
        messages.add(new OpenAiChatRequest.Message(role, content.trim()));
    }

    /**
     * 描述：截断日志中的长文本内容。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private String abbreviate(String value) {
        return StringUtils.abbreviate(StringUtils.defaultString(value), 1000);
    }

    private String cleanModelReply(String content) {
        return THINK_BLOCK_PATTERN.matcher(StringUtils.defaultString(content)).replaceAll("").trim();
    }

    /**
     * 描述：脱敏展示 API Key，便于排查配置是否生效但避免泄露完整密钥。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private String maskApiKey(String apiKey) {
        String value = StringUtils.trimToEmpty(apiKey);
        if (value.length() <= 10) {
            return "***";
        }
        return value.substring(0, 6) + "***" + value.substring(value.length() - 4);
    }

    private Optional<String> normalizeMessageRole(String role) {
        String value = StringUtils.trimToEmpty(role).toLowerCase();
        if (SUPPORTED_MESSAGE_ROLES.contains(value)) {
            return Optional.of(value);
        }
        log.debug("ai_reply_skip_unsupported_message_role role={}", role);
        return Optional.empty();
    }
}
