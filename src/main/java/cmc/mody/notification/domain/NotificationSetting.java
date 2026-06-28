package cmc.mody.notification.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notification_setting")
public class NotificationSetting extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "meal_reminder_enabled", nullable = false)
    private boolean mealReminderEnabled = true;

    @Column(name = "breakfast_time")
    private LocalTime breakfastTime;

    @Column(name = "lunch_time")
    private LocalTime lunchTime;

    @Column(name = "dinner_time")
    private LocalTime dinnerTime;

    @Column(name = "exercise_reminder_enabled", nullable = false)
    private boolean exerciseReminderEnabled = true;

    @Column(name = "exercise_time")
    private LocalTime exerciseTime;

    @Column(name = "streak_notification_enabled", nullable = false)
    private boolean streakNotificationEnabled = true;

    @Column(name = "comment_notification_enabled", nullable = false)
    private boolean commentNotificationEnabled = true;

    @Column(name = "challenge_notification_enabled", nullable = false)
    private boolean challengeNotificationEnabled = true;

    public NotificationSetting(Long id, Long memberId) {
        super(id);
        this.memberId = memberId;
    }

    public NotificationSetting(
        Long id,
        Long memberId,
        boolean mealReminderEnabled,
        LocalTime breakfastTime,
        LocalTime lunchTime,
        LocalTime dinnerTime,
        boolean exerciseReminderEnabled,
        LocalTime exerciseTime
    ) {
        this(id, memberId);
        this.mealReminderEnabled = mealReminderEnabled;
        this.breakfastTime = breakfastTime;
        this.lunchTime = lunchTime;
        this.dinnerTime = dinnerTime;
        this.exerciseReminderEnabled = exerciseReminderEnabled;
        this.exerciseTime = exerciseTime;
    }
}
