package com.example.wechatbot.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
/**
 * 描述：表示一条企业微信回调事件持久化记录。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class CallbackEventEntity {

    private Long id;
    private String dedupeKey;
    private String traceId;
    private String externalUserId;
    private String msgSignature;
    private String nonce;
    private String timestampValue;
    private boolean processed;
    private LocalDateTime createdAt;
}
