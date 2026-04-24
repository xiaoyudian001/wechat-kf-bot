package com.example.wechatbot.service;

import com.example.wechatbot.config.CallbackProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackIdempotencyServiceTest {

    @Test
    void shouldMarkSecondIdenticalCallbackAsDuplicate() {
        CallbackProperties callbackProperties = new CallbackProperties();
        callbackProperties.setDedupeTtlMinutes(10);

        CallbackIdempotencyService service = new CallbackIdempotencyService(
                callbackProperties,
                emptyPersistentProvider(),
                emptyRedisProvider()
        );

        boolean first = service.isDuplicate("sig", "1710000000", "nonce", "<xml>payload</xml>");
        boolean second = service.isDuplicate("sig", "1710000000", "nonce", "<xml>payload</xml>");

        assertThat(first).isFalse();
        assertThat(second).isTrue();
    }

    @Test
    void shouldBuildStableShortTraceIdForSameCallback() {
        CallbackProperties callbackProperties = new CallbackProperties();
        CallbackIdempotencyService service = new CallbackIdempotencyService(
                callbackProperties,
                emptyPersistentProvider(),
                emptyRedisProvider()
        );

        String first = service.buildTraceId("sig", "1710000000", "nonce", "<xml>payload</xml>");
        String second = service.buildTraceId("sig", "1710000000", "nonce", "<xml>payload</xml>");

        assertThat(first).hasSize(16);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldPreferPersistentBackendWhenAvailable() {
        CallbackProperties callbackProperties = new CallbackProperties();
        PersistentCallbackEventService persistentService = mock(PersistentCallbackEventService.class);
        when(persistentService.tryBeginProcessing(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        CallbackIdempotencyService service = new CallbackIdempotencyService(
                callbackProperties,
                persistentProviderWith(persistentService),
                emptyRedisProvider()
        );

        boolean duplicate = service.isDuplicate("sig", "1710000000", "nonce", "<xml>payload</xml>");

        assertThat(duplicate).isTrue();
        service.markProcessed("sig", "1710000000", "nonce", "<xml>payload</xml>", "user-1");
        verify(persistentService).markProcessed(anyString(), anyString(), anyString());
    }

    @Test
    void shouldUseRedisDedupeBeforePersistentBackend() {
        CallbackProperties callbackProperties = new CallbackProperties();
        PersistentCallbackEventService persistentService = mock(PersistentCallbackEventService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        when(redisCacheService.markCallbackIfAbsent(anyString())).thenReturn(false);

        CallbackIdempotencyService service = new CallbackIdempotencyService(
                callbackProperties,
                persistentProviderWith(persistentService),
                redisProviderWith(redisCacheService)
        );

        boolean duplicate = service.isDuplicate("sig", "1710000000", "nonce", "<xml>payload</xml>");

        assertThat(duplicate).isTrue();
        verify(persistentService, never()).tryBeginProcessing(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PersistentCallbackEventService> emptyPersistentProvider() {
        ObjectProvider<PersistentCallbackEventService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<PersistentCallbackEventService> persistentProviderWith(PersistentCallbackEventService service) {
        ObjectProvider<PersistentCallbackEventService> provider = mock(ObjectProvider.class);
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
