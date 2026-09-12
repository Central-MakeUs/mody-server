package cmc.mody.challenge.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "global_weekly_challenge",
        indexes = {
                @Index(name = "idx_global_weekly_challenge_period", columnList = "starts_on, ends_on")
        }
)
public class GlobalWeeklyChallenge extends BaseEntity {
    @Column(name = "challenge_id", nullable = false, unique = true)
    private Long challengeId;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    public GlobalWeeklyChallenge(Long id, Long challengeId, LocalDate startsOn, LocalDate endsOn) {
        this(id, challengeId, null, startsOn, endsOn);
    }

    public GlobalWeeklyChallenge(
        Long id,
        Long challengeId,
        String idempotencyKey,
        LocalDate startsOn,
        LocalDate endsOn
    ) {
        super(id);
        this.challengeId = challengeId;
        this.idempotencyKey = idempotencyKey;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    public void updatePeriod(LocalDate startsOn, LocalDate endsOn) {
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }
}
