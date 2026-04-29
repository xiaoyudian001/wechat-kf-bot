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
        CallbackEventEntity entity = new CallbackEventEntity();
        entity.setDedupeKey(dedupeKey);
        entity.setTraceId(traceId);
        entity.setMsgSignature(msgSignature);
        entity.setNonce(nonce);
        entity.setTimestampValue(timestampValue);
        entity.setProcessed(false);
        entity.setCreatedAt(LocalDateTime.now());
        return !callbackEventRepository.tryInsertProcessing(entity);
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

    /**
     * 描述：处理失败时回滚未完成占位，允许后续重试。
     *
     * @author wangjw
     * @date 2026-04-29
     */
    public void markFailed(String dedupeKey) {
        callbackEventRepository.deleteUnprocessed(dedupeKey);
    }
}
