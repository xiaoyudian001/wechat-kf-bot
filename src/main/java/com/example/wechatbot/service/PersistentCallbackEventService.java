package com.example.wechatbot.service;

import com.example.wechatbot.entity.CallbackEventEntity;
import com.example.wechatbot.repository.CallbackEventRepository;

import java.time.LocalDateTime;

/**
 * 描述：负责回调事件的数据库持久化处理。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class PersistentCallbackEventService {

    private final CallbackEventRepository callbackEventRepository;

    /**
     * 描述：创建持久化回调事件服务实例。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public PersistentCallbackEventService(CallbackEventRepository callbackEventRepository) {
        this.callbackEventRepository = callbackEventRepository;
    }

    /**
     * 描述：尝试登记回调处理开始状态并判断是否重复。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public boolean tryBeginProcessing(String dedupeKey, String traceId,
                                      String msgSignature, String nonce, String timestampValue) {
        if (callbackEventRepository.findByDedupeKey(dedupeKey).isPresent()) {
            return true;
        }

        CallbackEventEntity entity = new CallbackEventEntity();
        entity.setDedupeKey(dedupeKey);
        entity.setTraceId(traceId);
        entity.setMsgSignature(msgSignature);
        entity.setNonce(nonce);
        entity.setTimestampValue(timestampValue);
        entity.setProcessed(false);
        entity.setCreatedAt(LocalDateTime.now());
        callbackEventRepository.save(entity);
        return false;
    }

    /**
     * 描述：标记指定回调事件已处理完成。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void markProcessed(String dedupeKey, String traceId, String externalUserId) {
        callbackEventRepository.markProcessed(dedupeKey, traceId, externalUserId);
    }
}
