package cmc.mody.report.infrastructure.repository;

import cmc.mody.report.domain.RecordReport;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordReportRepository extends JpaRepository<RecordReport, Long> {
    Optional<RecordReport> findByReporterMemberIdAndRecordIdAndDeletedAtIsNull(Long reporterMemberId, Long recordId);

    boolean existsByReporterMemberIdAndRecordIdAndDeletedAtIsNull(Long reporterMemberId, Long recordId);

    List<RecordReport> findByRecordIdAndResolutionIsNullAndDeletedAtIsNull(Long recordId);

    /** 숨김·기각 처리용. 콘솔과 Slack에서 동시에 처리해도 한쪽만 대기 신고를 보도록 잠근다(LLD-0040). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM RecordReport r
        WHERE r.recordId = :recordId AND r.resolution IS NULL AND r.deletedAt IS NULL
        """)
    List<RecordReport> lockPendingByRecordId(@Param("recordId") Long recordId);

    Optional<RecordReport> findFirstByRecordIdAndResolutionIsNotNullAndDeletedAtIsNullOrderByResolvedAtDesc(
        Long recordId);

    long countByRecordIdAndDeletedAtIsNull(Long recordId);

    long countByRecordIdAndResolutionIsNullAndDeletedAtIsNull(Long recordId);
}
