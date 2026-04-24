package com.example.wechatbot.repository.jdbc;

import com.example.wechatbot.entity.ChatMessageEntity;
import com.example.wechatbot.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@ConditionalOnProperty(prefix = "persistence.jdbc", name = "enabled", havingValue = "true")
/**
 * 描述：使用 JdbcTemplate 实现会话消息仓储。
 *
 * @author wangjw
 * @date 2026-04-24
 */
public class JdbcChatMessageRepository implements ChatMessageRepository {

    private static final RowMapper<ChatMessageEntity> CHAT_MESSAGE_ROW_MAPPER = (rs, rowNum) -> {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(rs.getLong("id"));
        entity.setExternalUserId(rs.getString("external_user_id"));
        entity.setRole(rs.getString("role"));
        entity.setContent(rs.getString("content"));
        entity.setRoute(rs.getString("route"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        entity.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return entity;
    };

    private final JdbcTemplate jdbcTemplate;

    /**
     * 描述：创建 JDBC 会话消息仓储实例。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public JdbcChatMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    /**
     * 描述：保存一条会话消息记录。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public void save(ChatMessageEntity entity) {
        try {
            jdbcTemplate.update("""
                            INSERT INTO chat_message (external_user_id, role, content, route, created_at)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    entity.getExternalUserId(),
                    entity.getRole(),
                    entity.getContent(),
                    entity.getRoute(),
                    Timestamp.valueOf(entity.getCreatedAt()));
        } catch (DataAccessException e) {
            log.error("persist_chat_message_failed userId={}, role={}",
                    entity.getExternalUserId(), entity.getRole(), e);
            throw new IllegalStateException("failed to persist chat message", e);
        }
    }

    @Override
    /**
     * 描述：查询指定用户最近的会话消息并按时间正序返回。
     *
     * @author wangjw
     * @date 2026-04-24
     */
    public List<ChatMessageEntity> findRecentByExternalUserId(String externalUserId, int limit) {
        try {
            List<ChatMessageEntity> entities = new ArrayList<>(jdbcTemplate.query("""
                            SELECT id, external_user_id, role, content, route, created_at
                            FROM chat_message
                            WHERE external_user_id = ?
                            ORDER BY created_at DESC
                            LIMIT ?
                            """,
                    CHAT_MESSAGE_ROW_MAPPER,
                    externalUserId,
                    limit
            ));
            Collections.reverse(entities);
            return entities;
        } catch (DataAccessException e) {
            log.error("load_recent_chat_messages_failed userId={}, limit={}", externalUserId, limit, e);
            throw new IllegalStateException("failed to load recent chat messages", e);
        }
    }
}
