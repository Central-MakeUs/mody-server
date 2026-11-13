-- 응원 이모지 댓글은 내용 없이 이모지 id만 저장한다. 일반 댓글은 NULL이다.
ALTER TABLE record_comment
    ADD COLUMN emoji_id VARCHAR(20) NULL;
