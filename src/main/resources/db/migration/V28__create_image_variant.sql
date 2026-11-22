-- 기록·인증 사진의 축소본(목록용·상세용) 생성 작업과 결과.
-- 행 자체가 작업 큐이며, 스케줄러가 FOR UPDATE SKIP LOCKED로 선점해 처리한다.
CREATE TABLE IF NOT EXISTS image_variant (
    id               BIGINT       NOT NULL,
    created_at       DATETIME(6)  NULL,
    updated_at       DATETIME(6)  NULL,
    deleted_at       DATETIME(6)  NULL,
    status           VARCHAR(255) NULL,
    image_key        VARCHAR(500) NOT NULL,
    variant_status   VARCHAR(20)  NOT NULL,
    attempt_count    INT          NOT NULL DEFAULT 0,
    next_attempt_at  DATETIME(6)  NULL,
    picked_by        VARCHAR(100) NULL,
    picked_at        DATETIME(6)  NULL,
    thumbnail_key    VARCHAR(500) NULL,
    display_key      VARCHAR(500) NULL,
    original_bytes   BIGINT       NULL,
    thumbnail_bytes  BIGINT       NULL,
    display_bytes    BIGINT       NULL,
    processed_at     DATETIME(6)  NULL,
    last_error       VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_image_variant_image_key UNIQUE (image_key),
    INDEX idx_image_variant_dispatch (variant_status, next_attempt_at)
);
