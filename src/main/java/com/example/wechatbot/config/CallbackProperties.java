package com.example.wechatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "callback")
/**
 * 描述：承载企业微信回调处理相关配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class CallbackProperties {

    private int dedupeTtlMinutes = 10;
}
