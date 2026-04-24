package com.example.wechatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "redis-cache")
/**
 * 描述：承载 Redis 缓存开关、key 前缀和 TTL 配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class RedisCacheProperties {

    private boolean enabled = false;
    private String keyPrefix = "wechat-kf-bot";
    private long tokenTtlBufferSeconds = 120;
    private long callbackDedupeTtlMinutes = 10;
    private long conversationTtlHours = 24;
}
