CREATE TABLE admin_announcement (
    id BIGINT NOT NULL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(500) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_detail VARCHAR(2000),
    recipient_count INT NOT NULL,
    token_count INT NOT NULL,
    invalid_token_count INT NOT NULL,
    send_status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    idempotency_key VARCHAR(64) NOT NULL,
    created_by VARCHAR(320) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    CONSTRAINT uk_admin_announcement_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_admin_announcement_created_id ON admin_announcement (created_at, id);
