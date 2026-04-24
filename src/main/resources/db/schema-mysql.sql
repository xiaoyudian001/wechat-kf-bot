CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    external_user_id VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    route VARCHAR(64) NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_chat_message_user_created (external_user_id, created_at)
);

CREATE TABLE IF NOT EXISTS wecom_callback_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dedupe_key CHAR(64) NOT NULL,
    trace_id VARCHAR(32) NOT NULL,
    external_user_id VARCHAR(128) NULL,
    msg_signature VARCHAR(255) NOT NULL,
    nonce VARCHAR(128) NOT NULL,
    timestamp_value VARCHAR(64) NOT NULL,
    processed TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_wecom_callback_dedupe_key (dedupe_key),
    INDEX idx_wecom_callback_trace_id (trace_id),
    INDEX idx_wecom_callback_user_created (external_user_id, created_at)
);
