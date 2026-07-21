package cmc.mody.record.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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

    @Column(name = "crop_x", precision = 19, scale = 17)
    private BigDecimal cropX;

    @Column(name = "crop_y", precision = 19, scale = 17)
    private BigDecimal cropY;

    @Column(name = "crop_width", precision = 19, scale = 17)
    private BigDecimal cropWidth;

    @Column(name = "crop_height", precision = 19, scale = 17)
    private BigDecimal cropHeight;

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
        BigDecimal cropX,
        BigDecimal cropY,
        BigDecimal cropWidth,
        BigDecimal cropHeight,
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
        this.cropX = cropX;
        this.cropY = cropY;
        this.cropWidth = cropWidth;
        this.cropHeight = cropHeight;
        this.uploadedAt = uploadedAt;
    }

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
        this(
            id,
            memberId,
            groupId,
            recordType,
            mealTime,
            menu,
            exerciseDurationMinutes,
            exerciseName,
            imageKey,
            null,
            null,
            null,
            null,
            uploadedAt
        );
    }

    public static ActivityRecord meal(
        Long id,
        Long memberId,
        Long groupId,
        LocalTime mealTime,
        String menu,
        String imageKey,
        BigDecimal cropX,
        BigDecimal cropY,
        BigDecimal cropWidth,
        BigDecimal cropHeight,
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
            cropX,
            cropY,
            cropWidth,
            cropHeight,
            uploadedAt
        );
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
        return meal(id, memberId, groupId, mealTime, menu, imageKey, null, null, null, null, uploadedAt);
    }

    public static ActivityRecord exercise(
        Long id,
        Long memberId,
        Long groupId,
        Integer exerciseDurationMinutes,
        String exerciseName,
        String imageKey,
        BigDecimal cropX,
        BigDecimal cropY,
        BigDecimal cropWidth,
        BigDecimal cropHeight,
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
            cropX,
            cropY,
            cropWidth,
            cropHeight,
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
        return exercise(
            id,
            memberId,
            groupId,
            exerciseDurationMinutes,
            exerciseName,
            imageKey,
            null,
            null,
            null,
            null,
            uploadedAt
        );
    }
}
