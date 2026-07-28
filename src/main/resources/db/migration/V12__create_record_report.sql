CREATE TABLE IF NOT EXISTS record_report (
    id BIGINT NOT NULL,
    reporter_member_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    deleted_at DATETIME(6),
    status VARCHAR(255),
    active_reporter_member_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN deleted_at IS NULL THEN reporter_member_id
            ELSE NULL
        END
    ) STORED,
    active_record_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN deleted_at IS NULL THEN record_id
            ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (id),
    INDEX idx_record_report_reporter (reporter_member_id),
    INDEX idx_record_report_record (record_id),
    UNIQUE INDEX uk_record_report_active_reporter_record (active_reporter_member_id, active_record_id)
);
