-- 푸시 토큰은 소프트 삭제 대신 하드 삭제한다. 남아 있는 소프트 삭제 행을 지운다.
DELETE FROM member_push_token
WHERE deleted_at IS NOT NULL;

-- 같은 회원·기기 중복은 켜진 행 중 가장 최근에 갱신된 행만 남긴다.
DELETE token
FROM member_push_token token
JOIN (
    SELECT id
    FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY member_id, device_id
                   ORDER BY enabled DESC, updated_at DESC, id DESC
               ) AS row_num
        FROM member_push_token
    ) ranked
    WHERE ranked.row_num > 1
) duplicated ON duplicated.id = token.id;

ALTER TABLE member_push_token
    ADD CONSTRAINT uk_member_push_token_member_device UNIQUE (member_id, device_id);

DROP INDEX idx_member_push_token_device ON member_push_token;
