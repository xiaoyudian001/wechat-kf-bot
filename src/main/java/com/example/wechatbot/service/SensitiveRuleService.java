package com.example.wechatbot.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 描述：负责识别需要转人工处理的敏感问题。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class SensitiveRuleService {

    private static final List<String> HUMAN_HANDOFF_KEYWORDS = List.of(
            "退款",
            "退钱",
            "赔偿",
            "补偿",
            "投诉",
            "差评",
            "改房",
            "换房",
            "改价",
            "取消订单",
            "取消预订",
            "平台投诉",
            "申诉"
    );

    /**
     * 描述：判断用户文本是否需要转人工。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public boolean shouldHandoff(String userText) {
        if (userText == null || userText.isBlank()) {
            return false;
        }
        return HUMAN_HANDOFF_KEYWORDS.stream().anyMatch(userText::contains);
    }

    /**
     * 描述：生成转人工提示话术。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public String handoffReply() {
        return "这个问题需要人工客服进一步为您确认处理，我先帮您转人工，请您稍等一下。";
    }
}
