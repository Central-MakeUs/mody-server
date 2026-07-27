SET @group_member_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'group_member'
);

SET @member_table_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'member'
);

SET @group_member_display_profile_sync_sql = CASE
    WHEN @group_member_table_exists = 1 AND @member_table_exists = 1 THEN
        'UPDATE group_member gm
         JOIN member m ON m.id = gm.member_id
         SET gm.display_nickname = m.nickname,
             gm.display_profile_image_key = m.profile_image_key,
             gm.updated_at = NOW(6)
         WHERE gm.deleted_at IS NULL
           AND gm.group_member_status = ''JOINED''
           AND m.deleted_at IS NULL
           AND (
               NOT (gm.display_nickname <=> m.nickname)
               OR NOT (gm.display_profile_image_key <=> m.profile_image_key)
           )'
    ELSE 'SELECT 1'
END;

PREPARE group_member_display_profile_sync_statement FROM @group_member_display_profile_sync_sql;
EXECUTE group_member_display_profile_sync_statement;
DEALLOCATE PREPARE group_member_display_profile_sync_statement;
