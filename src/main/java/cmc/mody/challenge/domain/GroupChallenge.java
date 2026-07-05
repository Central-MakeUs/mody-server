package cmc.mody.challenge.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "group_challenge",
        indexes = {
                @Index(name = "idx_group_challenge_group_period", columnList = "group_id, starts_on, ends_on")
        }
)
public class GroupChallenge extends BaseEntity {
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupChallengeStatus groupChallengeStatus = GroupChallengeStatus.IN_PROGRESS;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public GroupChallenge(Long id, Long groupId, Long challengeId, LocalDate startsOn, LocalDate endsOn) {
        super(id);
        this.groupId = groupId;
        this.challengeId = challengeId;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    public void reset(LocalDateTime endedAt) {
        this.groupChallengeStatus = GroupChallengeStatus.RESET;
        this.endedAt = endedAt;
    }

    public void complete(LocalDateTime completedAt) {
        this.groupChallengeStatus = GroupChallengeStatus.COMPLETED;
        this.completedAt = completedAt;
        this.endedAt = completedAt;
    }

    public DayOfWeek getDueDayOfWeek() {
        return endsOn.getDayOfWeek();
    }
}
