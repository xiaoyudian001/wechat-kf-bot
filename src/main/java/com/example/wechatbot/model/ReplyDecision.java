package com.example.wechatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
/**
 * 描述：表示一次回复决策的路由和回复内容。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class ReplyDecision {

    private ReplyRoute route;
    private String reply;
}
