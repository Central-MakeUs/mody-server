SET @global_weekly_idempotency_key_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'global_weekly_challenge'
      AND column_name = 'idempotency_key'
);

SET @global_weekly_idempotency_key_sql = CASE
    WHEN @global_weekly_idempotency_key_exists = 0
        THEN 'ALTER TABLE global_weekly_challenge ADD COLUMN idempotency_key VARCHAR(100) NULL'
    ELSE 'SELECT 1'
END;

PREPARE global_weekly_idempotency_key_statement FROM @global_weekly_idempotency_key_sql;
EXECUTE global_weekly_idempotency_key_statement;
DEALLOCATE PREPARE global_weekly_idempotency_key_statement;

SET @global_weekly_idempotency_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'global_weekly_challenge'
      AND index_name = 'uk_global_weekly_challenge_idempotency_key'
);

SET @global_weekly_idempotency_index_sql = CASE
    WHEN @global_weekly_idempotency_index_exists = 0
        THEN 'CREATE UNIQUE INDEX uk_global_weekly_challenge_idempotency_key
              ON global_weekly_challenge (idempotency_key)'
    ELSE 'SELECT 1'
END;

PREPARE global_weekly_idempotency_index_statement FROM @global_weekly_idempotency_index_sql;
EXECUTE global_weekly_idempotency_index_statement;
DEALLOCATE PREPARE global_weekly_idempotency_index_statement;
