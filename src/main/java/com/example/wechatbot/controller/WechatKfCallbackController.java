package com.example.wechatbot.controller;

import com.example.wechatbot.model.ReplyDecision;
import com.example.wechatbot.service.AiReplyService;
import com.example.wechatbot.service.CallbackIdempotencyService;
import com.example.wechatbot.service.ChatLogService;
import com.example.wechatbot.service.ConversationMemoryService;
import com.example.wechatbot.service.KfMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.message.WxCpXmlMessage;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述：处理企业微信客服回调校验和消息接收。
 *
 * @author wangjw
 * @date 2026-04-24
 */
@Tag(name = "企业微信客服回调", description = "处理企业微信客服 URL 校验、消息回调和自动回复")
@Slf4j
@RestController
@RequestMapping("/wecom/kf")
@RequiredArgsConstructor
public class WechatKfCallbackController {

    private final WxCpService wxCpService;
    private final AiReplyService aiReplyService;
    private final KfMessageService kfMessageService;
    private final ConversationMemoryService conversationMemoryService;
    private final ChatLogService chatLogService;
    private final CallbackIdempotencyService callbackIdempotencyService;

    /**
     * 描述：处理企业微信回调 URL 验证请求。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Operation(summary = "企业微信回调 URL 校验", description = "企业微信后台保存回调地址时调用，用于解密并返回 echostr。")
    @GetMapping("/callback")
    public String verifyUrl(@Parameter(description = "企业微信消息签名", required = true)
                            @RequestParam("msg_signature") String msgSignature,
                            @Parameter(description = "企业微信回调时间戳", required = true)
                            @RequestParam("timestamp") String timestamp,
                            @Parameter(description = "企业微信回调随机串", required = true)
                            @RequestParam("nonce") String nonce,
                            @Parameter(description = "企业微信加密校验字符串", required = true)
                            @RequestParam("echostr") String echostr) {
        WxCpCryptUtil cryptUtil = new WxCpCryptUtil(wxCpService.getWxCpConfigStorage());
        return cryptUtil.decrypt(echostr, msgSignature, timestamp, nonce);
    }

    /**
     * 描述：接收企业微信客服消息回调并发送机器人回复。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    @Operation(summary = "接收企业微信客服消息回调", description = "解密企业微信客服消息，执行回复决策，并调用客服发送接口回复用户。")
    @PostMapping(value = "/callback", produces = "application/xml;charset=UTF-8")
    public String receive(@Parameter(description = "企业微信加密 XML 请求体", required = true)
                          @RequestBody String requestBody,
                          @Parameter(description = "企业微信消息签名", required = true)
                          @RequestParam("msg_signature") String msgSignature,
                          @Parameter(description = "企业微信回调时间戳", required = true)
                          @RequestParam("timestamp") String timestamp,
                          @Parameter(description = "企业微信回调随机串", required = true)
                          @RequestParam("nonce") String nonce) {
        String externalUserId = null;
        String userText = null;
        String traceId = callbackIdempotencyService.buildTraceId(msgSignature, timestamp, nonce, requestBody);
        MDC.put("traceId", traceId);

        try {
            if (callbackIdempotencyService.isDuplicate(msgSignature, timestamp, nonce, requestBody)) {
                chatLogService.logDuplicateCallback(traceId);
                return "success";
            }

            WxCpXmlMessage message = WxCpXmlMessage.fromEncryptedXml(
                    requestBody,
                    wxCpService.getWxCpConfigStorage(),
                    timestamp,
                    nonce,
                    msgSignature
            );
            log.info("wechat callback message={}", message);
            if (!"text".equals(message.getMsgType())) {
                log.info("ignore message type={}", message.getMsgType());
                return "success";
            }

            userText = message.getContent();
            externalUserId = message.getFromUserName();
            if (StringUtils.isBlank(externalUserId) || StringUtils.isBlank(userText)) {
                return "success";
            }

            ReplyDecision decision = aiReplyService.generateReply(externalUserId, userText);
            kfMessageService.sendText(externalUserId, decision.getReply());
            conversationMemoryService.addUserMessage(externalUserId, userText);
            conversationMemoryService.addAssistantMessage(externalUserId, decision.getReply(), decision.getRoute());
            callbackIdempotencyService.markProcessed(msgSignature, timestamp, nonce, requestBody, externalUserId);
            chatLogService.logReply(externalUserId, userText, decision.getRoute(), decision.getReply());
            return "success";
        } catch (Exception e) {
            chatLogService.logError(externalUserId, userText, e);
            callbackIdempotencyService.markFailed(msgSignature, timestamp, nonce, requestBody);
            return "failed";
        } finally {
            MDC.remove("traceId");
        }
    }
}
