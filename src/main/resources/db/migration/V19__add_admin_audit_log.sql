CREATE TABLE admin_audit_log (
    id BIGINT NOT NULL PRIMARY KEY,
    actor_admin_id BIGINT,
    actor_label VARCHAR(320) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    status_code INT NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_admin_audit_log_created_id ON admin_audit_log (created_at, id);
