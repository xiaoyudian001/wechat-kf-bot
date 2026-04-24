package com.example.wechatbot.service;

import com.example.wechatbot.config.CallbackProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Service
/**
 * 描述：负责企业微信回调的短期幂等判断和 traceId 生成。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class CallbackIdempotencyService {

    private final Cache<String, Boolean> processedCallbacks;
    private final PersistentCallbackEventService persistentCallbackEventService;
    private final RedisCacheService redisCacheService;

    /**
     * 描述：创建回调幂等服务并初始化内存去重缓存。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public CallbackIdempotencyService(CallbackProperties callbackProperties,
                                      ObjectProvider<PersistentCallbackEventService> persistentCallbackEventServiceProvider,
                                      ObjectProvider<RedisCacheService> redisCacheServiceProvider) {
        this.processedCallbacks = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(callbackProperties.getDedupeTtlMinutes()))
                .maximumSize(10_000)
                .build();
        this.persistentCallbackEventService = persistentCallbackEventServiceProvider.getIfAvailable();
        this.redisCacheService = redisCacheServiceProvider.getIfAvailable();
    }

    /**
     * 描述：判断当前回调是否已经处理过。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public boolean isDuplicate(String msgSignature, String timestamp, String nonce, String requestBody) {
        String key = buildKey(msgSignature, timestamp, nonce, requestBody);
        if (redisCacheService != null && !redisCacheService.markCallbackIfAbsent(key)) {
            return true;
        }
        if (persistentCallbackEventService != null) {
            return persistentCallbackEventService.tryBeginProcessing(key, buildTraceId(msgSignature, timestamp, nonce, requestBody),
                    msgSignature, nonce, timestamp);
        }
        return processedCallbacks.asMap().putIfAbsent(key, Boolean.TRUE) != null;
    }

    /**
     * 描述：将持久化回调事件标记为处理完成。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void markProcessed(String msgSignature, String timestamp, String nonce,
                              String requestBody, String externalUserId) {
        if (persistentCallbackEventService == null) {
            return;
        }

        String key = buildKey(msgSignature, timestamp, nonce, requestBody);
        String traceId = buildTraceId(msgSignature, timestamp, nonce, requestBody);
        persistentCallbackEventService.markProcessed(key, traceId, externalUserId);
    }

    /**
     * 描述：根据回调参数生成链路追踪 ID。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public String buildTraceId(String msgSignature, String timestamp, String nonce, String requestBody) {
        String hash = sha256Hex(msgSignature + "|" + timestamp + "|" + nonce + "|" + requestBody);
        return hash.substring(0, 16);
    }

    /**
     * 描述：根据回调参数生成幂等去重键。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private String buildKey(String msgSignature, String timestamp, String nonce, String requestBody) {
        return sha256Hex(msgSignature + "|" + timestamp + "|" + nonce + "|" + requestBody);
    }

    /**
     * 描述：对原始字符串计算 SHA-256 十六进制摘要。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private String sha256Hex(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("failed to build callback hash", e);
        }
    }
}
