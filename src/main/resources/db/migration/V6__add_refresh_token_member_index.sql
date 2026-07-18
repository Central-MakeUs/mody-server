SET @refresh_token_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_token'
);

SET @refresh_token_member_active_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_token'
      AND INDEX_NAME = 'idx_refresh_token_member_active'
);

SET @refresh_token_member_active_index_sql = CASE
    WHEN @refresh_token_table_exists = 1 AND @refresh_token_member_active_index_exists = 0
        THEN 'CREATE INDEX idx_refresh_token_member_active ON refresh_token (member_id, deleted_at, id)'
    ELSE 'SELECT 1'
END;

PREPARE refresh_token_member_active_index_statement FROM @refresh_token_member_active_index_sql;
EXECUTE refresh_token_member_active_index_statement;
DEALLOCATE PREPARE refresh_token_member_active_index_statement;
