package com.example.wechatbot.model;

/**
 * 描述：枚举机器人回复命中的处理路径。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public enum ReplyRoute {
    SENSITIVE_HANDOFF,
    ORDER_INFO_REQUIRED,
    ORDER_PENDING,
    FAQ_MATCH,
    AI_REPLY,
    FALLBACK_HANDOFF
}
