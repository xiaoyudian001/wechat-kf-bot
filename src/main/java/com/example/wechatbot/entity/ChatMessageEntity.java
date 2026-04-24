package com.example.wechatbot.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 描述：表示一条用户或机器人会话消息持久化记录。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class ChatMessageEntity {

    private Long id;
    private String externalUserId;
    private String role;
    private String content;
    private String route;
    private LocalDateTime createdAt;
}
