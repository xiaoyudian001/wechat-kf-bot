package com.example.wechatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
/**
 * 描述：承载 AI 服务地址、模型、密钥和知识库路径配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class AiProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
    private String systemPrompt;
    private String faqPath;
    private String faqJsonPath;
}
