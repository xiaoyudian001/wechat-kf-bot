package com.example.wechatbot.repository.jdbc;

import com.example.wechatbot.entity.CallbackEventEntity;
import com.example.wechatbot.repository.CallbackEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;

@Slf4j
@Repository
@ConditionalOnProperty(prefix = "persistence.jdbc", name = "enabled", havingValue = "true")
/**
 * 描述：使用 JdbcTemplate 实现回调事件仓储。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class JdbcCallbackEventRepository implements CallbackEventRepository {

    private static final RowMapper<CallbackEventEntity> CALLBACK_EVENT_ROW_MAPPER = (rs, rowNum) -> {
        CallbackEventEntity entity = new CallbackEventEntity();
        entity.setId(rs.getLong("id"));
        entity.setDedupeKey(rs.getString("dedupe_key"));
        entity.setTraceId(rs.getString("trace_id"));
        entity.setExternalUserId(rs.getString("external_user_id"));
        entity.setMsgSignature(rs.getString("msg_signature"));
        entity.setNonce(rs.getString("nonce"));
        entity.setTimestampValue(rs.getString("timestamp_value"));
        entity.setProcessed(rs.getBoolean("processed"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        entity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return entity;
    };

    private final JdbcTemplate jdbcTemplate;

    /**
     * 描述：创建 JDBC 回调事件仓储实例。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public JdbcCallbackEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    /**
     * 描述：根据去重键查询回调事件记录。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public boolean tryInsertProcessing(CallbackEventEntity entity) {
        try {
            int affectedRows = jdbcTemplate.update("""
                            INSERT IGNORE INTO wecom_callback_event (
                                dedupe_key, trace_id, external_user_id, msg_signature, nonce,
                                timestamp_value, processed, created_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    entity.getDedupeKey(),
                    entity.getTraceId(),
                    entity.getExternalUserId(),
                    entity.getMsgSignature(),
                    entity.getNonce(),
                    entity.getTimestampValue(),
                    entity.isProcessed(),
                    Timestamp.valueOf(entity.getCreatedAt()));
            return affectedRows > 0;
        } catch (DataAccessException e) {
            log.error("try_insert_callback_event_failed dedupeKey={}, traceId={}",
                    entity.getDedupeKey(), entity.getTraceId(), e);
            throw new IllegalStateException("failed to reserve callback event", e);
        }
    }

    @Override
    public void markProcessed(String dedupeKey, String traceId, String externalUserId) {
        try {
            jdbcTemplate.update("""
                            UPDATE wecom_callback_event
                            SET trace_id = ?, external_user_id = ?, processed = 1
                            WHERE dedupe_key = ?
                            """,
                    traceId,
                    externalUserId,
                    dedupeKey
            );
        } catch (DataAccessException e) {
            log.error("mark_callback_event_processed_failed dedupeKey={}, traceId={}", dedupeKey, traceId, e);
            throw new IllegalStateException("failed to mark callback event processed", e);
        }
    }

    @Override
    public void deleteUnprocessed(String dedupeKey) {
        try {
            jdbcTemplate.update("""
                            DELETE FROM wecom_callback_event
                            WHERE dedupe_key = ? AND processed = 0
                            """,
                    dedupeKey
            );
        } catch (DataAccessException e) {
            log.error("delete_unprocessed_callback_event_failed dedupeKey={}", dedupeKey, e);
            throw new IllegalStateException("failed to delete unprocessed callback event", e);
        }
    }

    @Override
    public Optional<CallbackEventEntity> findByDedupeKey(String dedupeKey) {
        try {
            return jdbcTemplate.query("""
                            SELECT id, dedupe_key, trace_id, external_user_id, msg_signature, nonce,
                                   timestamp_value, processed, created_at
                            FROM wecom_callback_event
                            WHERE dedupe_key = ?
                            LIMIT 1
                            """,
                    CALLBACK_EVENT_ROW_MAPPER,
                    dedupeKey
            ).stream().findFirst();
        } catch (DataAccessException e) {
            log.error("find_callback_event_failed dedupeKey={}", dedupeKey, e);
            throw new IllegalStateException("failed to query callback event", e);
        }
    }

}
