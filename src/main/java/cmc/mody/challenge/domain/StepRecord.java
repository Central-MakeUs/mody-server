package cmc.mody.challenge.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "step_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_step_record_challenge_member_date",
                        columnNames = {"group_challenge_id", "member_id", "recorded_on"}
                )
        },
        indexes = {
                @Index(name = "idx_step_record_challenge_date", columnList = "group_challenge_id, recorded_on"),
                @Index(name = "idx_step_record_member_date", columnList = "member_id, recorded_on")
        }
)
public class StepRecord extends BaseEntity {
    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "recorded_on", nullable = false)
    private LocalDate recordedOn;

    @Column(name = "step_count", nullable = false)
    private int stepCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepSource stepSource;

    public StepRecord(Long id, Long groupChallengeId, Long memberId, LocalDate recordedOn, int stepCount, StepSource stepSource) {
        super(id);
        this.groupChallengeId = groupChallengeId;
        this.memberId = memberId;
        this.recordedOn = recordedOn;
        this.stepCount = stepCount;
        this.stepSource = stepSource;
    }

    public void updateStepCount(int stepCount) {
        this.stepCount = stepCount;
    }
}
