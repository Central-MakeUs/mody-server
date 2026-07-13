package cmc.mody.record.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "activity_record_group",
        indexes = {
                @Index(name = "idx_activity_record_group_group_uploaded", columnList = "group_id, uploaded_at"),
                @Index(name = "idx_activity_record_group_record", columnList = "record_id"),
                @Index(name = "idx_activity_record_group_member", columnList = "member_id")
        }
)
public class ActivityRecordGroup extends BaseEntity {
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public ActivityRecordGroup(Long id, Long recordId, Long groupId, Long memberId, LocalDateTime uploadedAt) {
        super(id);
        this.recordId = recordId;
        this.groupId = groupId;
        this.memberId = memberId;
        this.uploadedAt = uploadedAt;
    }
}
