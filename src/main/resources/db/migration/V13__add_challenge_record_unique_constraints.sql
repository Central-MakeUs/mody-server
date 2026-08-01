SET @step_record_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'step_record'
);

SET @step_record_unique_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'step_record'
      AND index_name = 'uk_step_record_challenge_member_date'
);

SET @step_record_deduplicate_sql = CASE
    WHEN @step_record_table_exists = 1
        THEN 'DELETE duplicate_record FROM step_record duplicate_record JOIN step_record retained_record ON duplicate_record.group_challenge_id = retained_record.group_challenge_id AND duplicate_record.member_id = retained_record.member_id AND duplicate_record.recorded_on = retained_record.recorded_on AND duplicate_record.id < retained_record.id'
    ELSE 'SELECT 1'
END;

PREPARE step_record_deduplicate_statement FROM @step_record_deduplicate_sql;
EXECUTE step_record_deduplicate_statement;
DEALLOCATE PREPARE step_record_deduplicate_statement;

SET @step_record_unique_index_sql = CASE
    WHEN @step_record_table_exists = 1 AND @step_record_unique_index_exists = 0
        THEN 'CREATE UNIQUE INDEX uk_step_record_challenge_member_date ON step_record (group_challenge_id, member_id, recorded_on)'
    ELSE 'SELECT 1'
END;

PREPARE step_record_unique_index_statement FROM @step_record_unique_index_sql;
EXECUTE step_record_unique_index_statement;
DEALLOCATE PREPARE step_record_unique_index_statement;

SET @challenge_proof_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'challenge_proof'
);

SET @challenge_proof_unique_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'challenge_proof'
      AND index_name = 'uk_challenge_proof_group_challenge_member'
);

SET @challenge_proof_deduplicate_sql = CASE
    WHEN @challenge_proof_table_exists = 1
        THEN 'DELETE duplicate_proof FROM challenge_proof duplicate_proof JOIN challenge_proof retained_proof ON duplicate_proof.group_challenge_id = retained_proof.group_challenge_id AND duplicate_proof.member_id = retained_proof.member_id AND duplicate_proof.id > retained_proof.id'
    ELSE 'SELECT 1'
END;

PREPARE challenge_proof_deduplicate_statement FROM @challenge_proof_deduplicate_sql;
EXECUTE challenge_proof_deduplicate_statement;
DEALLOCATE PREPARE challenge_proof_deduplicate_statement;

SET @challenge_proof_unique_index_sql = CASE
    WHEN @challenge_proof_table_exists = 1 AND @challenge_proof_unique_index_exists = 0
        THEN 'CREATE UNIQUE INDEX uk_challenge_proof_group_challenge_member ON challenge_proof (group_challenge_id, member_id)'
    ELSE 'SELECT 1'
END;

PREPARE challenge_proof_unique_index_statement FROM @challenge_proof_unique_index_sql;
EXECUTE challenge_proof_unique_index_statement;
DEALLOCATE PREPARE challenge_proof_unique_index_statement;
