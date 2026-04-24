package com.example.wechatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wecom")
/**
 * 描述：承载企业微信客服接入配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class WecomProperties {

    private String corpId;
    private String token;
    private String aesKey;
    private String secret;
    private String kfAccountId;
}
