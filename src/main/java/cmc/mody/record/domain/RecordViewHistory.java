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
        name = "record_view_history",
        indexes = {
                @Index(
                        name = "idx_record_view_history_viewer_group_writer",
                        columnList = "viewer_member_id, group_id, writer_member_id"
                )
        }
)
public class RecordViewHistory extends BaseEntity {
    @Column(name = "viewer_member_id", nullable = false)
    private Long viewerMemberId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "writer_member_id", nullable = false)
    private Long writerMemberId;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    public RecordViewHistory(
        Long id,
        Long viewerMemberId,
        Long groupId,
        Long writerMemberId,
        LocalDateTime lastViewedAt
    ) {
        super(id);
        this.viewerMemberId = viewerMemberId;
        this.groupId = groupId;
        this.writerMemberId = writerMemberId;
        this.lastViewedAt = lastViewedAt;
    }

    public void updateLastViewedAt(LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }
}
