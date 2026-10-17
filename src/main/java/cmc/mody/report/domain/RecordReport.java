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
    name = "record_report",
    indexes = {
        @Index(name = "idx_record_report_reporter", columnList = "reporter_member_id"),
        @Index(name = "idx_record_report_record", columnList = "record_id"),
        @Index(name = "idx_record_report_resolution", columnList = "resolution, record_id")
    }
)
public class RecordReport extends BaseEntity {
    @Column(name = "reporter_member_id", nullable = false)
    private Long reporterMemberId;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    /** 관리자 처리 결과. NULL이면 처리 대기. */
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 20)
    private RecordReportResolution resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 320)
    private String resolvedBy;

    public RecordReport(Long id, Long reporterMemberId, Long recordId) {
        super(id);
        this.reporterMemberId = reporterMemberId;
        this.recordId = recordId;
    }

    public void resolve(RecordReportResolution resolution, String resolvedBy, LocalDateTime resolvedAt) {
        this.resolution = resolution;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
    }

    public boolean isPending() {
        return resolution == null;
    }
}
