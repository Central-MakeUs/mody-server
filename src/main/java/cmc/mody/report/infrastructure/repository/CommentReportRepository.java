package cmc.mody.report.infrastructure.repository;

import cmc.mody.report.domain.CommentReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    Optional<CommentReport> findByReporterMemberIdAndCommentIdAndDeletedAtIsNull(Long reporterMemberId, Long commentId);

    List<CommentReport> findByCommentIdAndResolutionIsNullAndDeletedAtIsNull(Long commentId);
}
