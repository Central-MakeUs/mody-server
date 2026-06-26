package cmc.mody.challenge.domain;

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
        name = "challenge_badge",
        indexes = {
                @Index(name = "idx_challenge_badge_group", columnList = "group_id"),
                @Index(name = "idx_challenge_badge_group_challenge", columnList = "group_challenge_id")
        }
)
public class ChallengeBadge extends BaseEntity {
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    public ChallengeBadge(Long id, Long groupId, Long challengeId, Long groupChallengeId, LocalDateTime issuedAt) {
        super(id);
        this.groupId = groupId;
        this.challengeId = challengeId;
        this.groupChallengeId = groupChallengeId;
        this.issuedAt = issuedAt;
    }
}
