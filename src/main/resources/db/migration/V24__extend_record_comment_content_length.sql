-- 모디 스티커 코드(:mody-xxx:)를 섞어 쓰도록 댓글 길이 제한을 200자로 늘린다.
ALTER TABLE record_comment
    MODIFY COLUMN content VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
