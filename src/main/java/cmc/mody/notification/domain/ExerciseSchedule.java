package cmc.mody.notification.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "exercise_schedule",
        indexes = {
                @Index(name = "idx_exercise_schedule_member_day", columnList = "member_id, day_of_week")
        }
)
public class ExerciseSchedule extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    public ExerciseSchedule(Long id, Long memberId, DayOfWeek dayOfWeek, LocalTime scheduledTime) {
        super(id);
        this.memberId = memberId;
        this.dayOfWeek = dayOfWeek;
        this.scheduledTime = scheduledTime;
    }
}
