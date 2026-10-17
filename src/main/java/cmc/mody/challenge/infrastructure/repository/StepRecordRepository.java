package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.StepRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StepRecordRepository extends JpaRepository<StepRecord, Long> {
    Optional<StepRecord> findByGroupChallengeIdAndMemberIdAndRecordedOnAndDeletedAtIsNull(
        Long groupChallengeId,
        Long memberId,
        LocalDate recordedOn
    );

    /**
     * 하루 걸음 수를 원자적으로 저장한다.
     *
     * <p>같은 (그룹 챌린지, 회원, 날짜) 기록이 있으면 새로 만들지 않는다. 조회 후 저장하면
     * 동시 요청이 둘 다 신규로 판단해 유니크 제약에서 실패하므로 한 문장으로 처리한다.
     * <ul>
     *   <li>활성 기록: 기존 값과 요청 값 중 큰 값을 유지한다 (누적 걸음 수는 줄지 않는다).</li>
     *   <li>삭제된 기록: 그룹을 나갔다 다시 들어온 경우다. 되살리고 요청 값으로 덮어쓴다.</li>
     * </ul>
     * step_count 갱신이 deleted_at 초기화보다 먼저 와야 삭제 여부를 올바르게 판단한다.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
        insert into step_record
            (id, group_challenge_id, member_id, recorded_on, step_count, step_source,
             status, deleted_at, created_at, updated_at)
        values
            (:id, :groupChallengeId, :memberId, :recordedOn, :stepCount, :stepSource,
             'ACTIVE', null, :now, :now)
        on duplicate key update
            step_count = case
                when deleted_at is null then greatest(step_count, values(step_count))
                else values(step_count)
            end,
            step_source = case when deleted_at is null then step_source else values(step_source) end,
            status = 'ACTIVE',
            deleted_at = null,
            updated_at = values(updated_at)
        """, nativeQuery = true)
    int upsertDailyStepCount(
        @Param("id") Long id,
        @Param("groupChallengeId") Long groupChallengeId,
        @Param("memberId") Long memberId,
        @Param("recordedOn") LocalDate recordedOn,
        @Param("stepCount") int stepCount,
        @Param("stepSource") String stepSource,
        @Param("now") LocalDateTime now
    );

    List<StepRecord> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<StepRecord> findByMemberIdAndGroupChallengeIdInAndDeletedAtIsNull(
        Long memberId,
        Collection<Long> groupChallengeIds
    );

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
