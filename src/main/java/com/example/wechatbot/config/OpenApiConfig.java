package com.example.wechatbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：配置 Knife4j/OpenAPI 接口文档分组和基础信息。
 *
 * @author wangjw
 * @date 2026-04-24
 */
@Configuration
public class OpenApiConfig {

    /**
     * 描述：创建项目接口文档基础信息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Bean
    public OpenAPI wechatKfBotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("微信客服机器人接口文档")
                        .description("用于本地调试企业微信客服机器人、AI 回复和回调接口。")
                        .version("0.0.1"));
    }

    /**
     * 描述：创建企业微信客服接口文档分组。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Bean
    public GroupedOpenApi wecomKfApiGroup() {
        return GroupedOpenApi.builder()
                .group("企业微信客服接口")
                .pathsToMatch("/wecom/kf/**")
                .build();
    }

    /**
     * 描述：创建本地开发测试接口文档分组。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Bean
    public GroupedOpenApi devApiGroup() {
        return GroupedOpenApi.builder()
                .group("本地测试接口")
                .pathsToMatch("/dev/**")
                .build();
    }
}
