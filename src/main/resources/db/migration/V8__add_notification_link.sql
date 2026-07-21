SET @notification_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
);

SET @notification_link_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
      AND COLUMN_NAME = 'link'
);

SET @notification_link_sql = CASE
    WHEN @notification_table_exists = 1 AND @notification_link_exists = 0
        THEN 'ALTER TABLE notification ADD COLUMN link VARCHAR(300) NULL'
    ELSE 'SELECT 1'
END;

PREPARE notification_link_statement FROM @notification_link_sql;
EXECUTE notification_link_statement;
DEALLOCATE PREPARE notification_link_statement;
