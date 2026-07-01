package cmc.mody.record.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "activity_record",
        indexes = {
                @Index(name = "idx_activity_record_group_uploaded", columnList = "group_id, uploaded_at"),
                @Index(name = "idx_activity_record_member_uploaded", columnList = "member_id, uploaded_at")
        }
)
public class ActivityRecord extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "group_id")
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 20)
    private RecordType recordType;

    @Column(name = "meal_time")
    private LocalTime mealTime;

    @Column(length = 100)
    private String menu;

    @Column(name = "exercise_duration_minutes")
    private Integer exerciseDurationMinutes;

    @Column(name = "exercise_name", length = 30)
    private String exerciseName;

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public ActivityRecord(
        Long id,
        Long memberId,
        Long groupId,
        RecordType recordType,
        LocalTime mealTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName,
        String imageKey,
        LocalDateTime uploadedAt
    ) {
        super(id);
        this.memberId = memberId;
        this.groupId = groupId;
        this.recordType = recordType;
        this.mealTime = mealTime;
        this.menu = menu;
        this.exerciseDurationMinutes = exerciseDurationMinutes;
        this.exerciseName = exerciseName;
        this.imageKey = imageKey;
        this.uploadedAt = uploadedAt;
    }

    public static ActivityRecord meal(
        Long id,
        Long memberId,
        Long groupId,
        LocalTime mealTime,
        String menu,
        String imageKey,
        LocalDateTime uploadedAt
    ) {
        return new ActivityRecord(
            id,
            memberId,
            groupId,
            RecordType.MEAL,
            mealTime,
            menu,
            null,
            null,
            imageKey,
            uploadedAt
        );
    }

    public static ActivityRecord exercise(
        Long id,
        Long memberId,
        Long groupId,
        Integer exerciseDurationMinutes,
        String exerciseName,
        String imageKey,
        LocalDateTime uploadedAt
    ) {
        return new ActivityRecord(
            id,
            memberId,
            groupId,
            RecordType.EXERCISE,
            null,
            null,
            exerciseDurationMinutes,
            exerciseName,
            imageKey,
            uploadedAt
        );
    }
}
