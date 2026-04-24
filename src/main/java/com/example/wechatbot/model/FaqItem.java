package com.example.wechatbot.model;

import lombok.Data;

import java.util.List;

@Data
/**
 * 描述：表示一条 FAQ 标准问答配置。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class FaqItem {

    private List<String> keywords;
    private String answer;
}
