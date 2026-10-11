CREATE TABLE admin_account (
    id BIGINT NOT NULL PRIMARY KEY,
    google_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    deleted_at DATETIME(6),
    status VARCHAR(20),
    CONSTRAINT uk_admin_account_google_subject UNIQUE (google_subject)
);

CREATE INDEX idx_activity_record_uploaded_id ON activity_record (uploaded_at, id);
CREATE INDEX idx_challenge_proof_uploaded_id ON challenge_proof (uploaded_at, id);
