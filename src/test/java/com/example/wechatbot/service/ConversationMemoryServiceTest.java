package com.example.wechatbot.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationMemoryServiceTest {

    @Test
    void shouldUseInMemoryModeWhenPersistentServiceIsAbsent() {
        ConversationMemoryService service = new ConversationMemoryService(emptyPersistentProvider(), emptyRedisProvider());

        service.addUserMessage("user-1", "你好");
        service.addAssistantMessage("user-1", "您好");

        Deque<ConversationMemoryService.ChatMessage> messages = service.getRecentMessages("user-1");

        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().role()).isEqualTo("user");
        assertThat(messages.getLast().role()).isEqualTo("assistant");
    }

    @Test
    void shouldPreferPersistentServiceWhenAvailable() {
        PersistentConversationMemoryService persistentService = mock(PersistentConversationMemoryService.class);
        when(persistentService.getRecentMessages("user-1", 6)).thenReturn(new java.util.ArrayDeque<>());

        ConversationMemoryService service = new ConversationMemoryService(
                persistentProviderWith(persistentService),
                emptyRedisProvider()
        );

        service.addUserMessage("user-1", "你好");
        service.addAssistantMessage("user-1", "您好");
        service.getRecentMessages("user-1");

        verify(persistentService).addUserMessage("user-1", "你好");
        verify(persistentService).addAssistantMessage(eq("user-1"), eq("您好"), eq(null));
        verify(persistentService).getRecentMessages("user-1", 6);
    }

    @Test
    void shouldReadRecentMessagesFromRedisBeforePersistentService() {
        PersistentConversationMemoryService persistentService = mock(PersistentConversationMemoryService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        java.util.ArrayDeque<ConversationMemoryService.ChatMessage> redisMessages = new java.util.ArrayDeque<>();
        redisMessages.offerLast(new ConversationMemoryService.ChatMessage("user", "你好"));
        when(redisCacheService.getRecentConversationMessages("user-1")).thenReturn(redisMessages);

        ConversationMemoryService service = new ConversationMemoryService(
                persistentProviderWith(persistentService),
                redisProviderWith(redisCacheService)
        );

        Deque<ConversationMemoryService.ChatMessage> messages = service.getRecentMessages("user-1");

        assertThat(messages).hasSize(1);
        verify(persistentService, never()).getRecentMessages("user-1", 6);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PersistentConversationMemoryService> emptyPersistentProvider() {
        ObjectProvider<PersistentConversationMemoryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PersistentConversationMemoryService> persistentProviderWith(PersistentConversationMemoryService service) {
        ObjectProvider<PersistentConversationMemoryService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedisCacheService> emptyRedisProvider() {
        ObjectProvider<RedisCacheService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<RedisCacheService> redisProviderWith(RedisCacheService service) {
        ObjectProvider<RedisCacheService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(service);
        return provider;
    }
}
