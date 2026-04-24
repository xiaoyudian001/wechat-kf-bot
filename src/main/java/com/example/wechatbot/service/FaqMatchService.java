package com.example.wechatbot.service;

import com.example.wechatbot.model.FaqItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
/**
 * 描述：负责根据关键词匹配 FAQ 标准答案。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class FaqMatchService {

    private final FaqLoaderService faqLoaderService;

    /**
     * 描述：匹配用户文本对应的 FAQ 答案。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public Optional<String> match(String userText) {
        if (userText == null || userText.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(userText);
        List<FaqItem> faqItems = faqLoaderService.loadFaqItems();

        return faqItems.stream()
                .filter(item -> item.getKeywords() != null && item.getKeywords().stream()
                        .map(this::normalize)
                        .anyMatch(normalized::contains))
                .map(FaqItem::getAnswer)
                .findFirst();
    }

    /**
     * 描述：标准化文本以提升关键词匹配稳定性。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private String normalize(String text) {
        return text == null ? "" : text.replace("？", "")
                .replace("?", "")
                .replace(" ", "")
                .trim()
                .toLowerCase();
    }
}
