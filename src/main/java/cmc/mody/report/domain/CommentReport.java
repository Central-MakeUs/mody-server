package cmc.mody.report.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "comment_report",
    indexes = {
        @Index(name = "idx_comment_report_reporter", columnList = "reporter_member_id"),
        @Index(name = "idx_comment_report_comment", columnList = "comment_id"),
        @Index(name = "idx_comment_report_resolution", columnList = "resolution, comment_id")
    }
)
public class CommentReport extends BaseEntity {
    @Column(name = "reporter_member_id", nullable = false)
    private Long reporterMemberId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /** 관리자 처리 결과. NULL이면 처리 대기. 기록 신고와 같은 값을 쓴다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 20)
    private RecordReportResolution resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 320)
    private String resolvedBy;

    public CommentReport(Long id, Long reporterMemberId, Long commentId, Long recordId, Long groupId) {
        super(id);
        this.reporterMemberId = reporterMemberId;
        this.commentId = commentId;
        this.recordId = recordId;
        this.groupId = groupId;
    }

    public void resolve(RecordReportResolution resolution, String resolvedBy, LocalDateTime resolvedAt) {
        this.resolution = resolution;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }
}
