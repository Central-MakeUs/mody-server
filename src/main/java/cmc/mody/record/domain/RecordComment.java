package cmc.mody.record.domain;

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
        name = "record_comment",
        indexes = {
                @Index(name = "idx_record_comment_record", columnList = "record_id")
        }
)
public class RecordComment extends BaseEntity {
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 100)
    private String content;

    public RecordComment(Long id, Long recordId, Long memberId, String content) {
        super(id);
        this.recordId = recordId;
        this.memberId = memberId;
        this.content = content;
    }
}
