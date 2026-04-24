package com.example.wechatbot.service;

import com.example.wechatbot.config.AiProperties;
import com.example.wechatbot.model.OpenAiChatResponse;
import com.example.wechatbot.model.ReplyDecision;
import com.example.wechatbot.model.ReplyRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReplyServiceTest {

    private AiProperties aiProperties;
    private KnowledgeService knowledgeService;
    private SensitiveRuleService sensitiveRuleService;
    private ConversationMemoryService conversationMemoryService;
    private OrderIntentService orderIntentService;
    private FaqMatchService faqMatchService;
    private RestTemplate restTemplate;
    private AiReplyService aiReplyService;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.setBaseUrl("https://example.test/chat/completions");
        aiProperties.setApiKey("test-api-key");
        aiProperties.setModel("test-model");
        aiProperties.setSystemPrompt("你是民宿微信客服助理。");

        knowledgeService = mock(KnowledgeService.class);
        sensitiveRuleService = mock(SensitiveRuleService.class);
        conversationMemoryService = new ConversationMemoryService(emptyPersistentProvider(), emptyRedisProvider());
        orderIntentService = mock(OrderIntentService.class);
        faqMatchService = mock(FaqMatchService.class);
        restTemplate = mock(RestTemplate.class);

        when(sensitiveRuleService.handoffReply()).thenReturn("这个问题需要人工客服进一步处理。");

        aiReplyService = new AiReplyService(
                aiProperties,
                knowledgeService,
                sensitiveRuleService,
                conversationMemoryService,
                orderIntentService,
                faqMatchService,
                restTemplate
        );
    }

    @Test
    void shouldHandoffSensitiveQuestionsBeforeOtherRoutes() {
        when(sensitiveRuleService.shouldHandoff("我要退款")).thenReturn(true);

        ReplyDecision decision = aiReplyService.generateReply("user-1", "我要退款");

        assertThat(decision.getRoute()).isEqualTo(ReplyRoute.SENSITIVE_HANDOFF);
        assertThat(decision.getReply()).contains("人工客服");
        verify(orderIntentService, never()).isOrderRelated(any());
        verify(restTemplate, never()).postForObject(any(String.class), any(), eq(OpenAiChatResponse.class));
    }

    @Test
    void shouldAskForOrderInfoWhenOrderQuestionHasMissingInfo() {
        when(orderIntentService.isOrderRelated("帮我查订单")).thenReturn(true);
        when(orderIntentService.hasEnoughOrderInfo("帮我查订单")).thenReturn(false);
        when(orderIntentService.askForOrderInfo()).thenReturn("请提供预订姓名、手机号后四位和入住日期。");

        ReplyDecision decision = aiReplyService.generateReply("user-1", "帮我查订单");

        assertThat(decision.getRoute()).isEqualTo(ReplyRoute.ORDER_INFO_REQUIRED);
        assertThat(decision.getReply()).contains("手机号后四位");
        verify(faqMatchService, never()).match(any());
        verify(restTemplate, never()).postForObject(any(String.class), any(), eq(OpenAiChatResponse.class));
    }

    @Test
    void shouldReturnFaqAnswerBeforeCallingAi() {
        when(faqMatchService.match("几点入住？")).thenReturn(Optional.of("您好，15:00后可以入住。"));

        ReplyDecision decision = aiReplyService.generateReply("user-1", "几点入住？");

        assertThat(decision.getRoute()).isEqualTo(ReplyRoute.FAQ_MATCH);
        assertThat(decision.getReply()).isEqualTo("您好，15:00后可以入住。");
        verify(knowledgeService, never()).getFaqContent();
        verify(restTemplate, never()).postForObject(any(String.class), any(), eq(OpenAiChatResponse.class));
    }

    @Test
    void shouldCallAiWhenRulesAndFaqDoNotMatch() {
        when(faqMatchService.match("附近有什么吃的？")).thenReturn(Optional.empty());
        when(knowledgeService.getFaqContent()).thenReturn("附近有本地餐馆。");
        when(restTemplate.postForObject(
                eq(aiProperties.getBaseUrl()),
                any(),
                eq(OpenAiChatResponse.class)
        )).thenReturn(chatResponse("附近有几家本地餐馆，可以步行前往。"));

        ReplyDecision decision = aiReplyService.generateReply("user-1", "附近有什么吃的？");

        assertThat(decision.getRoute()).isEqualTo(ReplyRoute.AI_REPLY);
        assertThat(decision.getReply()).isEqualTo("附近有几家本地餐馆，可以步行前往。");
    }

    @Test
    void shouldFallbackToHandoffWhenAiCallFails() {
        when(faqMatchService.match("附近有什么吃的？")).thenReturn(Optional.empty());
        when(knowledgeService.getFaqContent()).thenReturn("附近有本地餐馆。");
        when(restTemplate.postForObject(
                eq(aiProperties.getBaseUrl()),
                any(),
                eq(OpenAiChatResponse.class)
        )).thenThrow(new RestClientException("timeout"));

        ReplyDecision decision = aiReplyService.generateReply("user-1", "附近有什么吃的？");

        assertThat(decision.getRoute()).isEqualTo(ReplyRoute.FALLBACK_HANDOFF);
        assertThat(decision.getReply()).contains("人工客服");
    }

    private OpenAiChatResponse chatResponse(String content) {
        OpenAiChatResponse.Message message = new OpenAiChatResponse.Message();
        message.setRole("assistant");
        message.setContent(content);

        OpenAiChatResponse.Choice choice = new OpenAiChatResponse.Choice();
        choice.setMessage(message);

        OpenAiChatResponse response = new OpenAiChatResponse();
        response.setChoices(List.of(choice));
        return response;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PersistentConversationMemoryService> emptyPersistentProvider() {
        ObjectProvider<PersistentConversationMemoryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedisCacheService> emptyRedisProvider() {
        ObjectProvider<RedisCacheService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
