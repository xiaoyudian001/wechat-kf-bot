package com.example.wechatbot.model;

/**
 * 描述：枚举机器人回复命中的处理路径。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public enum ReplyRoute {
    SENSITIVE_HANDOFF,    // 敏感词转人工
    ORDER_INFO_REQUIRED,  // 需要订单信息
    ORDER_PENDING,        // 订单待处理
    FAQ_MATCH,            // 常见问题匹配
    AI_REPLY,             // AI自动回复
    FALLBACK_HANDOFF      // 默认转人工
}
