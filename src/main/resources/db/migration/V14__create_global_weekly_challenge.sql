CREATE TABLE global_weekly_challenge (
    id BIGINT NOT NULL,
    challenge_id BIGINT NOT NULL,
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    status VARCHAR(20) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_global_weekly_challenge_challenge (challenge_id),
    KEY idx_global_weekly_challenge_period (starts_on, ends_on)
);

ALTER TABLE group_challenge
    ADD COLUMN global_weekly_challenge_id BIGINT NULL;

CREATE INDEX idx_group_challenge_global_weekly
    ON group_challenge (global_weekly_challenge_id, group_id);
