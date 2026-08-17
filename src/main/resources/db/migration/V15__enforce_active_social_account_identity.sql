SET @social_account_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'social_account'
);

SET @social_account_deduplicate_sql = CASE
    WHEN @social_account_table_exists = 1 THEN
        'UPDATE social_account duplicate_account
         JOIN (
             SELECT id
             FROM (
                 SELECT active_account.id,
                        ROW_NUMBER() OVER (
                            PARTITION BY active_account.login_type, active_account.provider_user_id
                            ORDER BY
                                EXISTS (
                                    SELECT 1
                                    FROM group_member group_member
                                    WHERE group_member.member_id = active_account.member_id
                                      AND group_member.deleted_at IS NULL
                                      AND group_member.group_member_status = ''JOINED''
                                ) DESC,
                                EXISTS (
                                    SELECT 1
                                    FROM activity_record activity_record
                                    WHERE activity_record.member_id = active_account.member_id
                                      AND activity_record.deleted_at IS NULL
                                ) DESC,
                                active_account.created_at ASC,
                                active_account.id ASC
                        ) AS duplicate_rank
                 FROM social_account active_account
                 WHERE active_account.deleted_at IS NULL
             ) ranked_accounts
             WHERE duplicate_rank > 1
         ) duplicate_accounts ON duplicate_accounts.id = duplicate_account.id
         SET duplicate_account.status = ''INACTIVE'',
             duplicate_account.deleted_at = NOW(6),
             duplicate_account.updated_at = NOW(6)'
    ELSE 'SELECT 1'
END;

PREPARE social_account_deduplicate_statement FROM @social_account_deduplicate_sql;
EXECUTE social_account_deduplicate_statement;
DEALLOCATE PREPARE social_account_deduplicate_statement;

SET @active_provider_identity_hash_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'social_account'
      AND COLUMN_NAME = 'active_provider_identity_hash'
);

SET @active_provider_identity_hash_column_sql = CASE
    WHEN @social_account_table_exists = 1 AND @active_provider_identity_hash_column_exists = 0 THEN
        'ALTER TABLE social_account
         ADD COLUMN active_provider_identity_hash BINARY(32)
         GENERATED ALWAYS AS (
             CASE
                 WHEN deleted_at IS NULL THEN UNHEX(SHA2(CONCAT(login_type, '':'' , provider_user_id), 256))
                 ELSE NULL
             END
         ) STORED'
    ELSE 'SELECT 1'
END;

PREPARE active_provider_identity_hash_column_statement FROM @active_provider_identity_hash_column_sql;
EXECUTE active_provider_identity_hash_column_statement;
DEALLOCATE PREPARE active_provider_identity_hash_column_statement;

SET @active_provider_identity_hash_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'social_account'
      AND INDEX_NAME = 'uk_social_account_active_provider_identity'
);

SET @active_provider_identity_hash_index_sql = CASE
    WHEN @social_account_table_exists = 1 AND @active_provider_identity_hash_index_exists = 0 THEN
        'CREATE UNIQUE INDEX uk_social_account_active_provider_identity
         ON social_account (active_provider_identity_hash)'
    ELSE 'SELECT 1'
END;

PREPARE active_provider_identity_hash_index_statement FROM @active_provider_identity_hash_index_sql;
EXECUTE active_provider_identity_hash_index_statement;
DEALLOCATE PREPARE active_provider_identity_hash_index_statement;
