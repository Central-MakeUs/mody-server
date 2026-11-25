package cmc.mody.report.infrastructure.repository;

import cmc.mody.report.domain.CommentReport;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {
    Optional<CommentReport> findByReporterMemberIdAndCommentIdAndDeletedAtIsNull(Long reporterMemberId, Long commentId);

    List<CommentReport> findByCommentIdAndResolutionIsNullAndDeletedAtIsNull(Long commentId);

    /** 숨김·기각 처리용. 콘솔과 Slack에서 동시에 처리해도 한쪽만 대기 신고를 보도록 잠근다(LLD-0040). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM CommentReport c
        WHERE c.commentId = :commentId AND c.resolution IS NULL AND c.deletedAt IS NULL
        """)
    List<CommentReport> lockPendingByCommentId(@Param("commentId") Long commentId);

    Optional<CommentReport> findFirstByCommentIdAndResolutionIsNotNullAndDeletedAtIsNullOrderByResolvedAtDesc(
        Long commentId);

    long countByCommentIdAndDeletedAtIsNull(Long commentId);

    long countByCommentIdAndResolutionIsNullAndDeletedAtIsNull(Long commentId);
}
