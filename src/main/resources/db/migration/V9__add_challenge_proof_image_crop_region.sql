SET @challenge_proof_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'challenge_proof'
);

SET @challenge_proof_crop_x_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'challenge_proof'
      AND COLUMN_NAME = 'crop_x'
);

SET @challenge_proof_crop_y_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'challenge_proof'
      AND COLUMN_NAME = 'crop_y'
);

SET @challenge_proof_crop_width_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'challenge_proof'
      AND COLUMN_NAME = 'crop_width'
);

SET @challenge_proof_crop_height_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'challenge_proof'
      AND COLUMN_NAME = 'crop_height'
);

SET @challenge_proof_crop_x_sql = CASE
    WHEN @challenge_proof_table_exists = 1 AND @challenge_proof_crop_x_exists = 0
        THEN 'ALTER TABLE challenge_proof ADD COLUMN crop_x DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

SET @challenge_proof_crop_y_sql = CASE
    WHEN @challenge_proof_table_exists = 1 AND @challenge_proof_crop_y_exists = 0
        THEN 'ALTER TABLE challenge_proof ADD COLUMN crop_y DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

SET @challenge_proof_crop_width_sql = CASE
    WHEN @challenge_proof_table_exists = 1 AND @challenge_proof_crop_width_exists = 0
        THEN 'ALTER TABLE challenge_proof ADD COLUMN crop_width DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

SET @challenge_proof_crop_height_sql = CASE
    WHEN @challenge_proof_table_exists = 1 AND @challenge_proof_crop_height_exists = 0
        THEN 'ALTER TABLE challenge_proof ADD COLUMN crop_height DECIMAL(19,17) NULL'
    ELSE 'SELECT 1'
END;

PREPARE challenge_proof_crop_x_statement FROM @challenge_proof_crop_x_sql;
EXECUTE challenge_proof_crop_x_statement;
DEALLOCATE PREPARE challenge_proof_crop_x_statement;

PREPARE challenge_proof_crop_y_statement FROM @challenge_proof_crop_y_sql;
EXECUTE challenge_proof_crop_y_statement;
DEALLOCATE PREPARE challenge_proof_crop_y_statement;

PREPARE challenge_proof_crop_width_statement FROM @challenge_proof_crop_width_sql;
EXECUTE challenge_proof_crop_width_statement;
DEALLOCATE PREPARE challenge_proof_crop_width_statement;

PREPARE challenge_proof_crop_height_statement FROM @challenge_proof_crop_height_sql;
EXECUTE challenge_proof_crop_height_statement;
DEALLOCATE PREPARE challenge_proof_crop_height_statement;
