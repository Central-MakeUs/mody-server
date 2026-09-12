SET @active_global_weekly_challenge_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'group_challenge'
      AND column_name = 'active_global_weekly_challenge_id'
);

SET @active_global_weekly_challenge_column_sql = CASE
    WHEN @active_global_weekly_challenge_column_exists = 0
        THEN 'ALTER TABLE group_challenge
              ADD COLUMN active_global_weekly_challenge_id BIGINT GENERATED ALWAYS AS (
                  CASE
                      WHEN deleted_at IS NULL THEN global_weekly_challenge_id
                      ELSE NULL
                  END
              ) STORED'
    ELSE 'SELECT 1'
END;

PREPARE active_global_weekly_challenge_column_statement
    FROM @active_global_weekly_challenge_column_sql;
EXECUTE active_global_weekly_challenge_column_statement;
DEALLOCATE PREPARE active_global_weekly_challenge_column_statement;

SET @active_global_group_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'group_challenge'
      AND column_name = 'active_global_group_id'
);

SET @active_global_group_column_sql = CASE
    WHEN @active_global_group_column_exists = 0
        THEN 'ALTER TABLE group_challenge
              ADD COLUMN active_global_group_id BIGINT GENERATED ALWAYS AS (
                  CASE
                      WHEN deleted_at IS NULL THEN group_id
                      ELSE NULL
                  END
              ) STORED'
    ELSE 'SELECT 1'
END;

PREPARE active_global_group_column_statement FROM @active_global_group_column_sql;
EXECUTE active_global_group_column_statement;
DEALLOCATE PREPARE active_global_group_column_statement;

SET @active_global_group_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'group_challenge'
      AND index_name = 'uk_group_challenge_active_global_group'
);

SET @active_global_group_duplicate_count = (
    SELECT COUNT(*)
    FROM (
        SELECT active_global_weekly_challenge_id, active_global_group_id
        FROM group_challenge
        WHERE active_global_weekly_challenge_id IS NOT NULL
        GROUP BY active_global_weekly_challenge_id, active_global_group_id
        HAVING COUNT(*) > 1
    ) duplicates
);

SET @active_global_group_index_sql = CASE
    WHEN @active_global_group_index_exists = 0
        AND @active_global_group_duplicate_count = 0
        THEN 'CREATE UNIQUE INDEX uk_group_challenge_active_global_group
              ON group_challenge (active_global_weekly_challenge_id, active_global_group_id)'
    ELSE 'SELECT 1'
END;

PREPARE active_global_group_index_statement FROM @active_global_group_index_sql;
EXECUTE active_global_group_index_statement;
DEALLOCATE PREPARE active_global_group_index_statement;
