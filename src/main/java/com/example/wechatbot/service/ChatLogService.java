package com.example.wechatbot.service;

import com.example.wechatbot.model.ReplyRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
/**
 * 描述：负责记录聊天回复、重复回调和异常日志。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class ChatLogService {

    /**
     * 描述：记录重复回调日志。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void logDuplicateCallback(String traceId) {
        log.info("callback_duplicate traceId={}", safe(traceId));
    }

    /**
     * 描述：记录一次机器人回复日志。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void logReply(String externalUserId, String userText, ReplyRoute route, String reply) {
        log.info("chat_reply userId={}, route={}, userText={}, reply={}",
                safe(externalUserId), route, safe(userText), safe(reply));
    }

    /**
     * 描述：记录聊天处理异常日志。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void logError(String externalUserId, String userText, Exception e) {
        log.error("chat_error userId={}, userText={}", safe(externalUserId), safe(userText), e);
    }

    /**
     * 描述：清理日志字段中的换行和空值。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    private String safe(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\r\\n]+", " ").trim();
    }
}
