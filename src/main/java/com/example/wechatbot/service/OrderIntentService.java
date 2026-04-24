package com.example.wechatbot.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 描述：负责识别订单相关问题并判断订单信息是否充足。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class OrderIntentService {

    private static final List<String> ORDER_KEYWORDS = List.of(
            "订单",
            "预订",
            "入住",
            "退房",
            "房号",
            "房间号",
            "预留",
            "订房",
            "订了",
            "我明天到",
            "我后天到",
            "怎么入住",
            "入住方式",
            "密码锁",
            "密码",
            "预订成功",
            "查订单",
            "我的房间"
    );

    /**
     * 描述：判断用户文本是否属于订单相关问题。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public boolean isOrderRelated(String userText) {
        if (userText == null || userText.isBlank()) {
            return false;
        }
        return ORDER_KEYWORDS.stream().anyMatch(userText::contains);
    }

    /**
     * 描述：判断用户文本是否包含足够的订单核实信息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public boolean hasEnoughOrderInfo(String userText) {
        if (userText == null || userText.isBlank()) {
            return false;
        }

        boolean hasPhoneTail = userText.matches(".*\\d{4}.*");
        boolean hasDate = userText.contains("今天")
                || userText.contains("明天")
                || userText.contains("后天")
                || userText.matches(".*\\d{1,2}月\\d{1,2}日.*")
                || userText.matches(".*\\d{4}-\\d{1,2}-\\d{1,2}.*");

        return hasPhoneTail || hasDate;
    }

    /**
     * 描述：生成订单信息不足时的追问话术。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public String askForOrderInfo() {
        return "麻烦您提供一下预订姓名、手机号后四位和入住日期，我帮您核实订单信息。";
    }
}
