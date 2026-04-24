package com.example.wechatbot.model;

import lombok.Data;

import java.util.List;

@Data
/**
 * 描述：封装 OpenAI 兼容聊天补全响应。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class OpenAiChatResponse {

    private List<Choice> choices;

    @Data
    /**
     * 描述：表示大模型响应中的一个候选结果。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public static class Choice {
        private Message message;
    }

    @Data
    /**
     * 描述：表示大模型返回的消息内容。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public static class Message {
        private String role;
        private String content;
    }
}
