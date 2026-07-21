SET @activity_record_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record'
);

SET @activity_record_crop_x_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record'
      AND COLUMN_NAME = 'crop_x'
);

SET @activity_record_crop_y_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record'
      AND COLUMN_NAME = 'crop_y'
);

SET @activity_record_crop_width_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record'
      AND COLUMN_NAME = 'crop_width'
);

SET @activity_record_crop_height_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'activity_record'
      AND COLUMN_NAME = 'crop_height'
);

SET @activity_record_crop_x_sql = CASE
    WHEN @activity_record_table_exists = 1 AND @activity_record_crop_x_exists = 0
        THEN 'ALTER TABLE activity_record ADD COLUMN crop_x DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

SET @activity_record_crop_y_sql = CASE
    WHEN @activity_record_table_exists = 1 AND @activity_record_crop_y_exists = 0
        THEN 'ALTER TABLE activity_record ADD COLUMN crop_y DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

SET @activity_record_crop_width_sql = CASE
    WHEN @activity_record_table_exists = 1 AND @activity_record_crop_width_exists = 0
        THEN 'ALTER TABLE activity_record ADD COLUMN crop_width DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

SET @activity_record_crop_height_sql = CASE
    WHEN @activity_record_table_exists = 1 AND @activity_record_crop_height_exists = 0
        THEN 'ALTER TABLE activity_record ADD COLUMN crop_height DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

PREPARE activity_record_crop_x_statement FROM @activity_record_crop_x_sql;
EXECUTE activity_record_crop_x_statement;
DEALLOCATE PREPARE activity_record_crop_x_statement;

PREPARE activity_record_crop_y_statement FROM @activity_record_crop_y_sql;
EXECUTE activity_record_crop_y_statement;
DEALLOCATE PREPARE activity_record_crop_y_statement;

PREPARE activity_record_crop_width_statement FROM @activity_record_crop_width_sql;
EXECUTE activity_record_crop_width_statement;
DEALLOCATE PREPARE activity_record_crop_width_statement;

PREPARE activity_record_crop_height_statement FROM @activity_record_crop_height_sql;
EXECUTE activity_record_crop_height_statement;
DEALLOCATE PREPARE activity_record_crop_height_statement;
