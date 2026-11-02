SET @notification_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notification'
);

SET @notification_type_sql = CASE
    WHEN @notification_table_exists = 1
        THEN CONCAT(
            'ALTER TABLE notification MODIFY COLUMN notification_type ENUM(',
            '''ADMIN_ANNOUNCEMENT'',',
            '''BUDDY_EXERCISE_RECORDED'',',
            '''BUDDY_MEAL_RECORDED'',',
            '''BUDDY_NUDGE'',',
            '''CHALLENGE'',',
            '''COMMENT'',',
            '''COMMENT_CREATED'',',
            '''DEV_TEST'',',
            '''EXERCISE_REMINDER'',',
            '''GROUP_JOINED'',',
            '''GROUP_MEMBER_JOINED'',',
            '''GROUP_RECORD_STREAK_RISK'',',
            '''MEAL_REMINDER'',',
            '''NUDGE'',',
            '''RECORD_REMINDER'',',
            '''STEP_CHALLENGE_COMPLETED'',',
            '''STREAK'',',
            '''WEEKLY_CHALLENGE_COMPLETED''',
            ') NOT NULL'
        )
    ELSE 'SELECT 1'
END;

PREPARE notification_type_statement FROM @notification_type_sql;
EXECUTE notification_type_statement;
DEALLOCATE PREPARE notification_type_statement;
