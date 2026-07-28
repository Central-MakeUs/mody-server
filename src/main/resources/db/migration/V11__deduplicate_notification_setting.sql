SET @notification_setting_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_setting'
);

SET @notification_setting_deduplicate_sql = CASE
    WHEN @notification_setting_table_exists = 1 THEN
        'UPDATE notification_setting ns
         JOIN (
             SELECT member_id, MAX(id) AS keep_id
             FROM notification_setting
             WHERE deleted_at IS NULL
             GROUP BY member_id
             HAVING COUNT(*) > 1
         ) duplicated ON duplicated.member_id = ns.member_id
         SET ns.status = ''INACTIVE'',
             ns.deleted_at = NOW(6),
             ns.updated_at = NOW(6)
         WHERE ns.deleted_at IS NULL
           AND ns.id <> duplicated.keep_id'
    ELSE 'SELECT 1'
END;

PREPARE notification_setting_deduplicate_statement FROM @notification_setting_deduplicate_sql;
EXECUTE notification_setting_deduplicate_statement;
DEALLOCATE PREPARE notification_setting_deduplicate_statement;

SET @notification_setting_active_member_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_setting'
      AND COLUMN_NAME = 'active_member_id'
);

SET @notification_setting_active_member_column_sql = CASE
    WHEN @notification_setting_table_exists = 1 AND @notification_setting_active_member_column_exists = 0
        THEN 'ALTER TABLE notification_setting
              ADD COLUMN active_member_id BIGINT
              GENERATED ALWAYS AS (
                  CASE
                      WHEN deleted_at IS NULL THEN member_id
                      ELSE NULL
                  END
              ) STORED'
    ELSE 'SELECT 1'
END;

PREPARE notification_setting_active_member_column_statement
    FROM @notification_setting_active_member_column_sql;
EXECUTE notification_setting_active_member_column_statement;
DEALLOCATE PREPARE notification_setting_active_member_column_statement;

SET @notification_setting_active_member_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification_setting'
      AND INDEX_NAME = 'uk_notification_setting_active_member'
);

SET @notification_setting_active_member_index_sql = CASE
    WHEN @notification_setting_table_exists = 1 AND @notification_setting_active_member_index_exists = 0
        THEN 'CREATE UNIQUE INDEX uk_notification_setting_active_member
              ON notification_setting (active_member_id)'
    ELSE 'SELECT 1'
END;

PREPARE notification_setting_active_member_index_statement
    FROM @notification_setting_active_member_index_sql;
EXECUTE notification_setting_active_member_index_statement;
DEALLOCATE PREPARE notification_setting_active_member_index_statement;
