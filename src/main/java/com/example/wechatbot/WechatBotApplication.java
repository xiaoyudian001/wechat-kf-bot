package com.example.wechatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
/**
 * 描述：微信客服机器人 Spring Boot 应用启动入口。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class WechatBotApplication {

    /**
     * 描述：启动 Spring Boot 应用。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public static void main(String[] args) {
        SpringApplication.run(WechatBotApplication.class, args);
    }
}
