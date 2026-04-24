package com.example.wechatbot.controller;

import com.example.wechatbot.model.ReplyDecision;
import com.example.wechatbot.service.AiReplyService;
import com.example.wechatbot.service.ChatLogService;
import com.example.wechatbot.service.ConversationMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述：提供本地开发环境下的聊天回复测试接口。
 *
 * @author wangjw
 * @date 2026-04-24
 */
@Tag(name = "本地聊天测试", description = "用于本地调试规则、FAQ 和 AI 回复链路")
@Profile("dev")
@CrossOrigin(origins = {"http://localhost:5173", "null"})
@RestController
@RequestMapping("/dev/chat")
@RequiredArgsConstructor
public class DevChatController {

    private final AiReplyService aiReplyService;
    private final ConversationMemoryService conversationMemoryService;
    private final ChatLogService chatLogService;

    /**
     * 描述：接收测试文本并返回机器人回复结果。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Operation(summary = "测试机器人回复", description = "直接传入用户文本，返回命中的回复路由和回复内容。")
    @PostMapping("/reply")
    public DevChatResponse reply(@RequestBody DevChatRequest request) {
        String userId = StringUtils.defaultIfBlank(request.getUserId(), "dev-user");
        String text = StringUtils.trimToEmpty(request.getText());
        if (StringUtils.isBlank(text)) {
            return new DevChatResponse("INVALID_INPUT", "请输入要测试的问题。");
        }

        ReplyDecision decision = aiReplyService.generateReply(userId, text);
        conversationMemoryService.addUserMessage(userId, text);
        conversationMemoryService.addAssistantMessage(userId, decision.getReply(), decision.getRoute());
        chatLogService.logReply(userId, text, decision.getRoute(), decision.getReply());
        return new DevChatResponse(decision.getRoute().name(), decision.getReply());
    }

    /**
     * 描述：封装本地测试聊天请求参数。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Data
    @Schema(description = "本地测试聊天请求")
    public static class DevChatRequest {
        @Schema(description = "测试用户 ID，不传时默认 dev-user", example = "dev-user")
        private String userId;

        @Schema(description = "用户输入文本", example = "附近有什么吃的？")
        private String text;
    }

    /**
     * 描述：封装本地测试聊天响应结果。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Data
    @Schema(description = "本地测试聊天响应")
    public static class DevChatResponse {
        @Schema(description = "回复命中的路由", example = "AI_REPLY")
        private final String route;

        @Schema(description = "最终返回给用户的回复内容", example = "附近有几家本地餐馆，可以步行前往。")
        private final String reply;
    }
}
