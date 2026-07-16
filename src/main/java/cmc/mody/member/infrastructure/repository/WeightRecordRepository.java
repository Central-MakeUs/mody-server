package cmc.mody.member.infrastructure.repository;

import cmc.mody.member.domain.WeightRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    Optional<WeightRecord> findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(Long memberId);

    Optional<WeightRecord> findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnAscCreatedAtAsc(Long memberId);

    List<WeightRecord> findByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(Long memberId);

    List<WeightRecord> findByMemberIdAndDeletedAtIsNull(Long memberId);

    default Optional<WeightRecord> findLatestOnOrBeforeRecordedOn(Long memberId, LocalDate recordedOn) {
        return findLatestOnOrBeforeRecordedOn(memberId, recordedOn, PageRequest.of(0, 1))
            .stream()
            .findFirst();
    }

    @Query("""
        select wr
        from WeightRecord wr
        where wr.memberId = :memberId
          and wr.recordedOn <= :recordedOn
          and wr.deletedAt is null
        order by wr.recordedOn desc, wr.createdAt desc
        """)
    List<WeightRecord> findLatestOnOrBeforeRecordedOn(
        @Param("memberId") Long memberId,
        @Param("recordedOn") LocalDate recordedOn,
        Pageable pageable
    );
}
