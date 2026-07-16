SET @refresh_token_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_token'
);

SET @refresh_token_deduplicate_sql = CASE
    WHEN @refresh_token_table_exists = 1 THEN
        'UPDATE refresh_token rt
         JOIN (
             SELECT token, MAX(id) AS keep_id
             FROM refresh_token
             WHERE deleted_at IS NULL
             GROUP BY token
             HAVING COUNT(*) > 1
         ) keep_tokens ON keep_tokens.token = rt.token
         SET rt.status = ''INACTIVE'', rt.deleted_at = NOW(6)
         WHERE rt.deleted_at IS NULL
           AND rt.id <> keep_tokens.keep_id'
    ELSE 'SELECT 1'
END;

PREPARE refresh_token_deduplicate_statement FROM @refresh_token_deduplicate_sql;
EXECUTE refresh_token_deduplicate_statement;
DEALLOCATE PREPARE refresh_token_deduplicate_statement;

SET @refresh_token_token_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refresh_token'
      AND INDEX_NAME = 'idx_refresh_token_token'
);

SET @refresh_token_token_index_sql = CASE
    WHEN @refresh_token_table_exists = 1 AND @refresh_token_token_index_exists = 0
        THEN 'CREATE INDEX idx_refresh_token_token ON refresh_token (token)'
    ELSE 'SELECT 1'
END;

PREPARE refresh_token_token_index_statement FROM @refresh_token_token_index_sql;
EXECUTE refresh_token_token_index_statement;
DEALLOCATE PREPARE refresh_token_token_index_statement;
