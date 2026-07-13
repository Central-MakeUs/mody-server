CREATE TABLE IF NOT EXISTS activity_record_group (
    id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    uploaded_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

SET @record_comment_group_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'record_comment'
      AND COLUMN_NAME = 'group_id'
);

SET @record_comment_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'record_comment'
);

SET @record_comment_group_column_sql = CASE
    WHEN @record_comment_table_exists = 1 AND @record_comment_group_column_exists = 0
        THEN 'ALTER TABLE record_comment ADD COLUMN group_id BIGINT NULL'
    ELSE 'SELECT 1'
END;

PREPARE record_comment_group_column_statement FROM @record_comment_group_column_sql;
EXECUTE record_comment_group_column_statement;
DEALLOCATE PREPARE record_comment_group_column_statement;

SET @record_comment_group_backfill_sql = CASE
    WHEN @record_comment_table_exists = 1
        THEN 'UPDATE record_comment rc JOIN activity_record ar ON ar.id = rc.record_id SET rc.group_id = ar.group_id WHERE rc.group_id IS NULL AND ar.group_id IS NOT NULL'
    ELSE 'SELECT 1'
END;

PREPARE record_comment_group_backfill_statement FROM @record_comment_group_backfill_sql;
EXECUTE record_comment_group_backfill_statement;
DEALLOCATE PREPARE record_comment_group_backfill_statement;

SET @record_comment_group_null_count = 0;

SET @record_comment_group_null_count_sql = CASE
    WHEN @record_comment_table_exists = 1
        THEN 'SELECT COUNT(*) INTO @record_comment_group_null_count FROM record_comment WHERE group_id IS NULL'
    ELSE 'SELECT 1'
END;

PREPARE record_comment_group_null_count_statement FROM @record_comment_group_null_count_sql;
EXECUTE record_comment_group_null_count_statement;
DEALLOCATE PREPARE record_comment_group_null_count_statement;

SET @record_comment_group_not_null_sql = CASE
    WHEN @record_comment_table_exists = 1 AND @record_comment_group_null_count = 0
        THEN 'ALTER TABLE record_comment MODIFY COLUMN group_id BIGINT NOT NULL'
    ELSE 'SELECT 1'
END;

PREPARE record_comment_group_not_null_statement FROM @record_comment_group_not_null_sql;
EXECUTE record_comment_group_not_null_statement;
DEALLOCATE PREPARE record_comment_group_not_null_statement;

SET @activity_record_group_group_uploaded_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record_group'
      AND INDEX_NAME = 'idx_activity_record_group_group_uploaded'
);

SET @activity_record_group_group_uploaded_index_sql = CASE
    WHEN @activity_record_group_group_uploaded_index_exists = 0
        THEN 'CREATE INDEX idx_activity_record_group_group_uploaded ON activity_record_group (group_id, uploaded_at)'
    ELSE 'SELECT 1'
END;

PREPARE activity_record_group_group_uploaded_index_statement FROM @activity_record_group_group_uploaded_index_sql;
EXECUTE activity_record_group_group_uploaded_index_statement;
DEALLOCATE PREPARE activity_record_group_group_uploaded_index_statement;

SET @activity_record_group_record_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record_group'
      AND INDEX_NAME = 'idx_activity_record_group_record'
);

SET @activity_record_group_record_index_sql = CASE
    WHEN @activity_record_group_record_index_exists = 0
        THEN 'CREATE INDEX idx_activity_record_group_record ON activity_record_group (record_id)'
    ELSE 'SELECT 1'
END;

PREPARE activity_record_group_record_index_statement FROM @activity_record_group_record_index_sql;
EXECUTE activity_record_group_record_index_statement;
DEALLOCATE PREPARE activity_record_group_record_index_statement;

SET @activity_record_group_member_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record_group'
      AND INDEX_NAME = 'idx_activity_record_group_member'
);

SET @activity_record_group_member_index_sql = CASE
    WHEN @activity_record_group_member_index_exists = 0
        THEN 'CREATE INDEX idx_activity_record_group_member ON activity_record_group (member_id)'
    ELSE 'SELECT 1'
END;

PREPARE activity_record_group_member_index_statement FROM @activity_record_group_member_index_sql;
EXECUTE activity_record_group_member_index_statement;
DEALLOCATE PREPARE activity_record_group_member_index_statement;

SET @record_comment_group_record_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'record_comment'
      AND INDEX_NAME = 'idx_record_comment_group_record'
);

SET @record_comment_group_record_index_sql = CASE
    WHEN @record_comment_table_exists = 1 AND @record_comment_group_record_index_exists = 0
        THEN 'CREATE INDEX idx_record_comment_group_record ON record_comment (group_id, record_id)'
    ELSE 'SELECT 1'
END;

PREPARE record_comment_group_record_index_statement FROM @record_comment_group_record_index_sql;
EXECUTE record_comment_group_record_index_statement;
DEALLOCATE PREPARE record_comment_group_record_index_statement;
