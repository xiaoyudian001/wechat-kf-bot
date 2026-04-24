package com.example.wechatbot.service;

import com.example.wechatbot.config.AiProperties;
import com.example.wechatbot.model.FaqItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * 描述：负责从资源文件加载 FAQ 配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class FaqLoaderService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private List<FaqItem> cachedFaqItems;

    /**
     * 描述：加载并缓存 FAQ 问答列表。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public List<FaqItem> loadFaqItems() {
        if (cachedFaqItems != null) {
            return cachedFaqItems;
        }

        try (InputStream inputStream = new ClassPathResource(aiProperties.getFaqJsonPath()).getInputStream()) {
            cachedFaqItems = objectMapper.readValue(inputStream, new TypeReference<List<FaqItem>>() { });
            return cachedFaqItems;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
