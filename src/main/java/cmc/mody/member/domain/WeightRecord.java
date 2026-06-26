package cmc.mody.member.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "weight_record",
        indexes = {
                @Index(name = "idx_weight_record_member_date", columnList = "member_id, recorded_on")
        }
)
public class WeightRecord extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "recorded_on", nullable = false)
    private LocalDate recordedOn;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "change_from_previous_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal changeFromPreviousKg;

    public WeightRecord(Long id, Long memberId, LocalDate recordedOn, BigDecimal weightKg, BigDecimal changeFromPreviousKg) {
        super(id);
        this.memberId = memberId;
        this.recordedOn = recordedOn;
        this.weightKg = weightKg;
        this.changeFromPreviousKg = changeFromPreviousKg;
    }
}
