package com.example.wechatbot.service;

import com.example.wechatbot.entity.ChatMessageEntity;
import com.example.wechatbot.model.ReplyRoute;
import com.example.wechatbot.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 描述：负责会话消息的数据库持久化读写。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class PersistentConversationMemoryService {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * 描述：创建持久化会话记忆服务实例。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public PersistentConversationMemoryService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    /**
     * 描述：保存用户消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void addUserMessage(String externalUserId, String content) {
        chatMessageRepository.save(buildEntity(externalUserId, "user", content, null));
    }

    /**
     * 描述：保存机器人回复消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void addAssistantMessage(String externalUserId, String content, ReplyRoute route) {
        chatMessageRepository.save(buildEntity(externalUserId, "assistant", content, route));
    }

    /**
     * 描述：查询最近会话消息并转换为上下文对象。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public Deque<ConversationMemoryService.ChatMessage> getRecentMessages(String externalUserId, int limit) {
        Deque<ConversationMemoryService.ChatMessage> messages = new ArrayDeque<>();
        for (ChatMessageEntity entity : chatMessageRepository.findRecentByExternalUserId(externalUserId, limit)) {
            messages.offerLast(new ConversationMemoryService.ChatMessage(entity.getRole(), entity.getContent()));
        }
        return messages;
    }

    /**
     * 描述：构建会话消息持久化实体。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private ChatMessageEntity buildEntity(String externalUserId, String role, String content, ReplyRoute route) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setExternalUserId(externalUserId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setRoute(route == null ? null : route.name());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
