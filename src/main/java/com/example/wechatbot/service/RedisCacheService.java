package com.example.wechatbot.service;

import com.example.wechatbot.config.RedisCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "redis-cache", name = "enabled", havingValue = "true")
/**
 * 描述：负责 Redis 中短期状态缓存的读写。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class RedisCacheService {

    private static final int MAX_MESSAGES = 6;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisCacheProperties properties;

    /**
     * 描述：创建 Redis 缓存服务实例。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public RedisCacheService(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             RedisCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 描述：从 Redis 获取企业微信 access_token。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public Optional<String> getAccessToken() {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key("wecom:access_token")));
        } catch (Exception e) {
            log.warn("redis_get_access_token_failed", e);
            return Optional.empty();
        }
    }

    /**
     * 描述：保存企业微信 access_token 到 Redis。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void saveAccessToken(String accessToken, long expiresInSeconds) {
        long ttlSeconds = Math.max(60, expiresInSeconds - properties.getTokenTtlBufferSeconds());
        try {
            redisTemplate.opsForValue().set(key("wecom:access_token"), accessToken, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("redis_save_access_token_failed ttlSeconds={}", ttlSeconds, e);
        }
    }

    /**
     * 描述：使用 Redis SETNX 判断回调是否首次出现。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public boolean markCallbackIfAbsent(String dedupeKey) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    key("callback:dedupe:" + dedupeKey),
                    "1",
                    Duration.ofMinutes(properties.getCallbackDedupeTtlMinutes())
            );
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("redis_callback_dedupe_failed dedupeKey={}", dedupeKey, e);
            return true;
        }
    }

    /**
     * 描述：追加一条最近会话消息到 Redis。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void addConversationMessage(String externalUserId, String role, String content) {
        String redisKey = key("chat:recent:" + externalUserId);
        try {
            String value = objectMapper.writeValueAsString(new ConversationMemoryService.ChatMessage(role, content));
            redisTemplate.opsForList().rightPush(redisKey, value);
            redisTemplate.opsForList().trim(redisKey, -MAX_MESSAGES, -1);
            redisTemplate.expire(redisKey, Duration.ofHours(properties.getConversationTtlHours()));
        } catch (JsonProcessingException e) {
            log.warn("redis_conversation_serialize_failed userId={}", externalUserId, e);
        } catch (Exception e) {
            log.warn("redis_add_conversation_failed userId={}", externalUserId, e);
        }
    }

    /**
     * 描述：从 Redis 获取指定用户最近会话消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public Deque<ConversationMemoryService.ChatMessage> getRecentConversationMessages(String externalUserId) {
        Deque<ConversationMemoryService.ChatMessage> messages = new ArrayDeque<>();
        try {
            List<String> values = redisTemplate.opsForList().range(key("chat:recent:" + externalUserId), 0, -1);
            if (values == null) {
                return messages;
            }
            for (String value : values) {
                messages.offerLast(objectMapper.readValue(value, ConversationMemoryService.ChatMessage.class));
            }
            return messages;
        } catch (Exception e) {
            log.warn("redis_get_conversation_failed userId={}", externalUserId, e);
            return new ArrayDeque<>();
        }
    }

    /**
     * 描述：将数据库中的最近会话预热到 Redis。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void warmConversationMessages(String externalUserId, Deque<ConversationMemoryService.ChatMessage> messages) {
        if (messages.isEmpty()) {
            return;
        }
        String redisKey = key("chat:recent:" + externalUserId);
        try {
            redisTemplate.delete(redisKey);
            for (ConversationMemoryService.ChatMessage message : messages) {
                String value = objectMapper.writeValueAsString(message);
                redisTemplate.opsForList().rightPush(redisKey, value);
            }
            redisTemplate.opsForList().trim(redisKey, -MAX_MESSAGES, -1);
            redisTemplate.expire(redisKey, Duration.ofHours(properties.getConversationTtlHours()));
        } catch (Exception e) {
            log.warn("redis_warm_conversation_failed userId={}", externalUserId, e);
        }
    }

    /**
     * 描述：根据统一前缀生成 Redis key。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private String key(String suffix) {
        return properties.getKeyPrefix() + ":" + suffix;
    }
}
