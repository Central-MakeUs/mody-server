package cmc.mody.grouping.domain;

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
        name = "group_member",
        indexes = {
                @Index(name = "idx_group_member_member", columnList = "member_id"),
                @Index(name = "idx_group_member_group", columnList = "group_id")
        }
)
public class GroupMember extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMemberStatus groupMemberStatus = GroupMemberStatus.JOINED;

    @Column(name = "display_nickname", length = 14)
    private String displayNickname;

    @Column(name = "display_profile_image_key", length = 500)
    private String displayProfileImageKey;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public GroupMember(Long id, Long memberId, Long groupId, LocalDateTime joinedAt) {
        super(id);
        this.memberId = memberId;
        this.groupId = groupId;
        this.joinedAt = joinedAt;
    }
}
