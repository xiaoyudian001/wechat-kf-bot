package com.example.wechatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "http.client")
/**
 * 描述：承载 HTTP 客户端连接和读取超时配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class HttpClientProperties {

    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 10000;
}
