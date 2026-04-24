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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        String faqContent = knowledgeService.getFaqContent();
        List<OpenAiChatRequest.Message> messages = new ArrayList<>();
        messages.add(new OpenAiChatRequest.Message("system", aiProperties.getSystemPrompt()));
        messages.add(new OpenAiChatRequest.Message("system",
                "以下是民宿知识库，请优先依据这些信息回答。如果知识库没有明确说明，不要编造。\n\n" + faqContent));

        for (ConversationMemoryService.ChatMessage msg : conversationMemoryService.getRecentMessages(externalUserId)) {
            messages.add(new OpenAiChatRequest.Message(msg.role(), msg.content()));
        }
        messages.add(new OpenAiChatRequest.Message("user", userText));

        OpenAiChatRequest request = new OpenAiChatRequest();
        request.setModel(aiProperties.getModel());
        request.setMessages(messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<OpenAiChatRequest> entity = new HttpEntity<>(request, headers);
        OpenAiChatResponse response;
        try {
            response = restTemplate.postForObject(
                    aiProperties.getBaseUrl(),
                    entity,
                    OpenAiChatResponse.class
            );
        } catch (RestClientResponseException e) {
            log.error("ai_reply_http_failed baseUrl={}, model={}, status={}, responseBody={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel(),
                    e.getStatusCode(),
                    abbreviate(e.getResponseBodyAsString()),
                    e);
            return new ReplyDecision(ReplyRoute.FALLBACK_HANDOFF, sensitiveRuleService.handoffReply());
        } catch (RestClientException e) {
            log.error("ai_reply_request_failed baseUrl={}, model={}, error={}",
                    aiProperties.getBaseUrl(),
                    aiProperties.getModel(),
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

        return new ReplyDecision(ReplyRoute.AI_REPLY, response.getChoices().get(0).getMessage().getContent().trim());
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
}
