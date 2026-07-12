SET @mody_group_code_length = (
    SELECT CHARACTER_MAXIMUM_LENGTH
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'mody_group'
      AND COLUMN_NAME = 'code'
);

SET @mody_group_code_sql = CASE
    WHEN @mody_group_code_length IS NULL THEN 'SELECT 1'
    WHEN @mody_group_code_length < 8 THEN 'ALTER TABLE mody_group MODIFY COLUMN code VARCHAR(8) NOT NULL'
    ELSE 'SELECT 1'
END;

PREPARE mody_group_code_statement FROM @mody_group_code_sql;
EXECUTE mody_group_code_statement;
DEALLOCATE PREPARE mody_group_code_statement;
