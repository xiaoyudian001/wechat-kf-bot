package com.example.wechatbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
/**
 * 描述：封装 OpenAI 兼容聊天补全请求。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class OpenAiChatRequest {

    private String model;
    private List<Message> messages;

    @Data
    @AllArgsConstructor
    /**
     * 描述：表示一条发送给大模型的消息。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public static class Message {
        private String role;
        private String content;
    }
}
