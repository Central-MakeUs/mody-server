package cmc.mody.challenge.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "step_challenge_detail")
public class StepChallengeDetail extends BaseEntity {
    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    @Column(name = "departure", nullable = false, length = 30)
    private String departure;

    @Column(name = "destination", nullable = false, length = 30)
    private String destination;

    @Column(name = "distance_km", nullable = false, precision = 6, scale = 1)
    private BigDecimal distanceKm;

    @Column(name = "target_step_count", nullable = false)
    private int targetStepCount;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public StepChallengeDetail(
            Long id,
            Long challengeId,
            String departure,
            String destination,
            BigDecimal distanceKm,
            int targetStepCount,
            int displayOrder
    ) {
        super(id);
        this.challengeId = challengeId;
        this.departure = departure;
        this.destination = destination;
        this.distanceKm = distanceKm;
        this.targetStepCount = targetStepCount;
        this.displayOrder = displayOrder;
    }
}
