package com.example.wechatbot.repository;

import com.example.wechatbot.entity.CallbackEventEntity;

import java.util.Optional;

/**
 * 描述：定义回调事件持久化访问能力。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public interface CallbackEventRepository {

    /**
     * 描述：原子地尝试登记一条处理中回调事件。
     *
     * @author wangjw
     * @date 2026-04-29
     */
    boolean tryInsertProcessing(CallbackEventEntity entity);

    /**
     * 描述：标记指定回调事件已处理完成。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    void markProcessed(String dedupeKey, String traceId, String externalUserId);

    /**
     * 描述：删除未处理完成的回调事件占位记录。
     *
     * @author wangjw
     * @date 2026-04-29
     */
    void deleteUnprocessed(String dedupeKey);

    /**
     * 描述：根据去重键查询回调事件。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    Optional<CallbackEventEntity> findByDedupeKey(String dedupeKey);
}
