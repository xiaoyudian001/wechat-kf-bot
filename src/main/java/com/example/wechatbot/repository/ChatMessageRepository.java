package com.example.wechatbot.repository;

import com.example.wechatbot.entity.ChatMessageEntity;

import java.util.List;

/**
 * 描述：定义会话消息持久化访问能力。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public interface ChatMessageRepository {

    /**
     * 描述：保存一条会话消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    void save(ChatMessageEntity entity);

    /**
     * 描述：查询指定用户最近的会话消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    List<ChatMessageEntity> findRecentByExternalUserId(String externalUserId, int limit);
}
