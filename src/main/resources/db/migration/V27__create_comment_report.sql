-- 기록 댓글 신고. 한 회원은 같은 댓글을 한 번만 신고한다. resolution이 NULL이면 처리 대기.
CREATE TABLE IF NOT EXISTS comment_report (
    id BIGINT NOT NULL,
    reporter_member_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    resolution VARCHAR(20) NULL,
    resolved_at DATETIME(6) NULL,
    resolved_by VARCHAR(320) NULL,
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
    active_comment_id BIGINT GENERATED ALWAYS AS (
        CASE
            WHEN deleted_at IS NULL THEN comment_id
            ELSE NULL
        END
    ) STORED,
    PRIMARY KEY (id),
    INDEX idx_comment_report_reporter (reporter_member_id),
    INDEX idx_comment_report_comment (comment_id),
    INDEX idx_comment_report_resolution (resolution, comment_id),
    UNIQUE INDEX uk_comment_report_active_reporter_comment (active_reporter_member_id, active_comment_id)
);
