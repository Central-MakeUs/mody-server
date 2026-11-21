-- 텍스트 사이에 응원 이모지 토큰(:clap:)을 섞어 쓰도록 댓글 길이 제한을 500자로 늘린다.
ALTER TABLE record_comment
    MODIFY COLUMN content VARCHAR(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
