package com.example.wechatbot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiConfigDiagnostics {

    private final AiProperties aiProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void logAiConfig() {
        String envApiKey = environment.getProperty("AI_API_KEY");
        log.info("ai_config_loaded baseUrl={}, model={}, minimalTestMode={}, configuredApiKey={}, envAiApiKey={}",
                aiProperties.getBaseUrl(),
                aiProperties.getModel(),
                aiProperties.isMinimalTestMode(),
                maskApiKey(aiProperties.getApiKey()),
                maskApiKey(envApiKey));
    }

    private String maskApiKey(String apiKey) {
        String value = StringUtils.trimToEmpty(apiKey);
        if (StringUtils.isBlank(value)) {
            return "<empty>";
        }
        if (value.startsWith("${") && value.endsWith("}")) {
            return "<unresolved-placeholder>";
        }
        if (value.length() <= 10) {
            return "***";
        }
        return value.substring(0, 6) + "***" + value.substring(value.length() - 4) + "(len=" + value.length() + ")";
    }
}
