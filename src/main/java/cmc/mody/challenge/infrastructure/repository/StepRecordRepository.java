package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.StepRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StepRecordRepository extends JpaRepository<StepRecord, Long> {
    @Query("""
        select coalesce(sum(record.stepCount), 0)
        from StepRecord record
        where record.groupChallengeId = :groupChallengeId
          and record.deletedAt is null
        """)
    long sumStepCountByGroupChallengeId(@Param("groupChallengeId") Long groupChallengeId);

    @Query("""
        select record.memberId, coalesce(sum(record.stepCount), 0)
        from StepRecord record
        where record.groupChallengeId = :groupChallengeId
          and record.deletedAt is null
        group by record.memberId
        """)
    List<Object[]> sumStepCountByMember(@Param("groupChallengeId") Long groupChallengeId);
}
