package cmc.mody.report.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
        @Index(name = "idx_record_report_record", columnList = "record_id")
    }
)
public class RecordReport extends BaseEntity {
    @Column(name = "reporter_member_id", nullable = false)
    private Long reporterMemberId;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    public RecordReport(Long id, Long reporterMemberId, Long recordId) {
        super(id);
        this.reporterMemberId = reporterMemberId;
        this.recordId = recordId;
    }
}
