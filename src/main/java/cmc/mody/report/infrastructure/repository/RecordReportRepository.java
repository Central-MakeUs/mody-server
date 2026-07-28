package cmc.mody.report.infrastructure.repository;

import cmc.mody.report.domain.RecordReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordReportRepository extends JpaRepository<RecordReport, Long> {
    Optional<RecordReport> findByReporterMemberIdAndRecordIdAndDeletedAtIsNull(Long reporterMemberId, Long recordId);

    boolean existsByReporterMemberIdAndRecordIdAndDeletedAtIsNull(Long reporterMemberId, Long recordId);
}
