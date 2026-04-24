package com.example.wechatbot.service;

import com.example.wechatbot.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
/**
 * 描述：负责加载提供给大模型的文本知识库。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class KnowledgeService {

    private final AiProperties aiProperties;
    private String faqContent;

    /**
     * 描述：获取并缓存 FAQ 文本知识库内容。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public String getFaqContent() {
        if (faqContent != null) {
            return faqContent;
        }

        try (InputStream inputStream = new ClassPathResource(aiProperties.getFaqPath()).getInputStream()) {
            faqContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return faqContent;
        } catch (Exception e) {
            throw new IllegalStateException("failed to load faq text", e);
        }
    }
}
