SET @member_push_token_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member_push_token'
);

SET @member_push_token_disabled_cleanup_sql = CASE
    WHEN @member_push_token_table_exists = 1 THEN
        'UPDATE member_push_token
         SET status = ''INACTIVE'', deleted_at = NOW(6)
         WHERE enabled = 0
           AND deleted_at IS NULL'
    ELSE 'SELECT 1'
END;

PREPARE member_push_token_disabled_cleanup_statement FROM @member_push_token_disabled_cleanup_sql;
EXECUTE member_push_token_disabled_cleanup_statement;
DEALLOCATE PREPARE member_push_token_disabled_cleanup_statement;

SET @member_push_token_fcm_deduplicate_sql = CASE
    WHEN @member_push_token_table_exists = 1 THEN
        'UPDATE member_push_token mpt
         JOIN (
             SELECT fcm_token, MAX(id) AS keep_id
             FROM member_push_token
             WHERE deleted_at IS NULL
             GROUP BY fcm_token
             HAVING COUNT(*) > 1
         ) duplicated ON duplicated.fcm_token = mpt.fcm_token
         SET mpt.enabled = 0, mpt.status = ''INACTIVE'', mpt.deleted_at = NOW(6)
         WHERE mpt.deleted_at IS NULL
           AND mpt.id <> duplicated.keep_id'
    ELSE 'SELECT 1'
END;

PREPARE member_push_token_fcm_deduplicate_statement FROM @member_push_token_fcm_deduplicate_sql;
EXECUTE member_push_token_fcm_deduplicate_statement;
DEALLOCATE PREPARE member_push_token_fcm_deduplicate_statement;

SET @member_push_token_device_deduplicate_sql = CASE
    WHEN @member_push_token_table_exists = 1 THEN
        'UPDATE member_push_token mpt
         JOIN (
             SELECT member_id, COALESCE(device_id, ''__NULL__'') AS normalized_device_id, MAX(id) AS keep_id
             FROM member_push_token
             WHERE deleted_at IS NULL
             GROUP BY member_id, COALESCE(device_id, ''__NULL__'')
             HAVING COUNT(*) > 1
         ) duplicated ON duplicated.member_id = mpt.member_id
             AND duplicated.normalized_device_id = COALESCE(mpt.device_id, ''__NULL__'')
         SET mpt.enabled = 0, mpt.status = ''INACTIVE'', mpt.deleted_at = NOW(6)
         WHERE mpt.deleted_at IS NULL
           AND mpt.id <> duplicated.keep_id'
    ELSE 'SELECT 1'
END;

PREPARE member_push_token_device_deduplicate_statement FROM @member_push_token_device_deduplicate_sql;
EXECUTE member_push_token_device_deduplicate_statement;
DEALLOCATE PREPARE member_push_token_device_deduplicate_statement;

SET @member_push_token_active_fcm_hash_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member_push_token'
      AND COLUMN_NAME = 'active_fcm_token_hash'
);

SET @member_push_token_active_fcm_hash_column_sql = CASE
    WHEN @member_push_token_table_exists = 1 AND @member_push_token_active_fcm_hash_column_exists = 0
        THEN 'ALTER TABLE member_push_token
              ADD COLUMN active_fcm_token_hash BINARY(32)
              GENERATED ALWAYS AS (
                  CASE
                      WHEN deleted_at IS NULL THEN UNHEX(SHA2(fcm_token, 256))
                      ELSE NULL
                  END
              ) STORED'
    ELSE 'SELECT 1'
END;

PREPARE member_push_token_active_fcm_hash_column_statement FROM @member_push_token_active_fcm_hash_column_sql;
EXECUTE member_push_token_active_fcm_hash_column_statement;
DEALLOCATE PREPARE member_push_token_active_fcm_hash_column_statement;

SET @member_push_token_active_member_device_hash_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member_push_token'
      AND COLUMN_NAME = 'active_member_device_hash'
);

SET @member_push_token_active_member_device_hash_column_sql = CASE
    WHEN @member_push_token_table_exists = 1 AND @member_push_token_active_member_device_hash_column_exists = 0
        THEN 'ALTER TABLE member_push_token
              ADD COLUMN active_member_device_hash BINARY(32)
              GENERATED ALWAYS AS (
                  CASE
                      WHEN deleted_at IS NULL THEN UNHEX(SHA2(CONCAT(member_id, '':'', COALESCE(device_id, ''__NULL__'')), 256))
                      ELSE NULL
                  END
              ) STORED'
    ELSE 'SELECT 1'
END;

PREPARE member_push_token_active_member_device_hash_column_statement
    FROM @member_push_token_active_member_device_hash_column_sql;
EXECUTE member_push_token_active_member_device_hash_column_statement;
DEALLOCATE PREPARE member_push_token_active_member_device_hash_column_statement;

SET @member_push_token_active_fcm_token_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member_push_token'
      AND INDEX_NAME = 'uk_member_push_token_active_fcm_token'
);

SET @member_push_token_active_fcm_token_index_sql = CASE
    WHEN @member_push_token_table_exists = 1 AND @member_push_token_active_fcm_token_index_exists = 0
        THEN 'CREATE UNIQUE INDEX uk_member_push_token_active_fcm_token
              ON member_push_token (active_fcm_token_hash)'
    ELSE 'SELECT 1'
END;

PREPARE member_push_token_active_fcm_token_index_statement FROM @member_push_token_active_fcm_token_index_sql;
EXECUTE member_push_token_active_fcm_token_index_statement;
DEALLOCATE PREPARE member_push_token_active_fcm_token_index_statement;

SET @member_push_token_active_member_device_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member_push_token'
      AND INDEX_NAME = 'uk_member_push_token_active_member_device'
);

SET @member_push_token_active_member_device_index_sql = CASE
    WHEN @member_push_token_table_exists = 1 AND @member_push_token_active_member_device_index_exists = 0
        THEN 'CREATE UNIQUE INDEX uk_member_push_token_active_member_device
              ON member_push_token (active_member_device_hash)'
    ELSE 'SELECT 1'
END;

PREPARE member_push_token_active_member_device_index_statement
    FROM @member_push_token_active_member_device_index_sql;
EXECUTE member_push_token_active_member_device_index_statement;
DEALLOCATE PREPARE member_push_token_active_member_device_index_statement;
