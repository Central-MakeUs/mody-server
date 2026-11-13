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
                @Index(name = "idx_record_comment_record", columnList = "record_id"),
                @Index(name = "idx_record_comment_group_record", columnList = "group_id, record_id")
        }
)
public class RecordComment extends BaseEntity {
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(name = "emoji_id", length = 20)
    private String emojiId;

    public RecordComment(Long id, Long recordId, Long groupId, Long memberId, String content) {
        this(id, recordId, groupId, memberId, content, null);
    }

    public RecordComment(Long id, Long recordId, Long groupId, Long memberId, String content, String emojiId) {
        super(id);
        this.recordId = recordId;
        this.groupId = groupId;
        this.memberId = memberId;
        this.content = content;
        this.emojiId = emojiId;
    }
}
