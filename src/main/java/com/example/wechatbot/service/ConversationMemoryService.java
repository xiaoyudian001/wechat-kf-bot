package com.example.wechatbot.service;

import com.example.wechatbot.model.ReplyRoute;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * 描述：负责维护用户最近会话上下文，支持 Redis、数据库和内存模式。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class ConversationMemoryService {

    private static final int MAX_MESSAGES = 6;
    private final Map<String, Deque<ChatMessage>> memory = new ConcurrentHashMap<>();
    private final PersistentConversationMemoryService persistentConversationMemoryService;
    private final RedisCacheService redisCacheService;

    /**
     * 描述：创建会话记忆服务并注入可选持久化和 Redis 能力。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public ConversationMemoryService(ObjectProvider<PersistentConversationMemoryService> persistentConversationMemoryServiceProvider,
                                     ObjectProvider<RedisCacheService> redisCacheServiceProvider) {
        this.persistentConversationMemoryService = persistentConversationMemoryServiceProvider.getIfAvailable();
        this.redisCacheService = redisCacheServiceProvider.getIfAvailable();
    }

    /**
     * 描述：追加用户消息到会话上下文。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void addUserMessage(String userId, String content) {
        if (persistentConversationMemoryService != null) {
            persistentConversationMemoryService.addUserMessage(userId, content);
            addRedisMessage(userId, "user", content);
            return;
        }
        addRedisMessage(userId, "user", content);
        addInMemoryMessage(userId, "user", content);
    }

    /**
     * 描述：追加机器人消息到会话上下文。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void addAssistantMessage(String userId, String content) {
        addAssistantMessage(userId, content, null);
    }

    /**
     * 描述：追加带回复路由的机器人消息到会话上下文。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void addAssistantMessage(String userId, String content, ReplyRoute route) {
        if (persistentConversationMemoryService != null) {
            persistentConversationMemoryService.addAssistantMessage(userId, content, route);
            addRedisMessage(userId, "assistant", content);
            return;
        }
        addRedisMessage(userId, "assistant", content);
        addInMemoryMessage(userId, "assistant", content);
    }

    /**
     * 描述：获取指定用户最近的会话消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public Deque<ChatMessage> getRecentMessages(String userId) {
        if (redisCacheService != null) {
            Deque<ChatMessage> redisMessages = redisCacheService.getRecentConversationMessages(userId);
            if (!redisMessages.isEmpty()) {
                return redisMessages;
            }
        }
        if (persistentConversationMemoryService != null) {
            Deque<ChatMessage> persistentMessages = persistentConversationMemoryService.getRecentMessages(userId, MAX_MESSAGES);
            if (redisCacheService != null) {
                redisCacheService.warmConversationMessages(userId, persistentMessages);
            }
            return persistentMessages;
        }
        return new ArrayDeque<>(memory.getOrDefault(userId, new ArrayDeque<>()));
    }

    /**
     * 描述：将消息追加到 Redis 最近会话缓存。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private void addRedisMessage(String userId, String role, String content) {
        if (redisCacheService != null) {
            redisCacheService.addConversationMessage(userId, role, content);
        }
    }

    /**
     * 描述：将消息追加到本地内存最近会话缓存。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private void addInMemoryMessage(String userId, String role, String content) {
        Deque<ChatMessage> deque = memory.computeIfAbsent(userId, key -> new ArrayDeque<>());
        if (deque.size() >= MAX_MESSAGES) {
            deque.pollFirst();
        }
        deque.offerLast(new ChatMessage(role, content));
    }

    /**
     * 描述：表示会话上下文中的一条消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public record ChatMessage(String role, String content) {
    }
}
