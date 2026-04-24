package com.example.wechatbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "persistence.jdbc")
/**
 * 描述：承载 JDBC 持久化开关配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class PersistenceProperties {

    private boolean enabled = false;
}
