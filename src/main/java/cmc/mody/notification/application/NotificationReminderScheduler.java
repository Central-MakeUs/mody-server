package cmc.mody.notification.application;

import cmc.mody.notification.domain.ExerciseSchedule;
import cmc.mody.notification.domain.MealType;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.infrastructure.repository.ExerciseScheduleRepository;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.reminder", name = "enabled", havingValue = "true")
public class NotificationReminderScheduler {
    private final NotificationSettingRepository notificationSettingRepository;
    private final ExerciseScheduleRepository exerciseScheduleRepository;
    private final NotificationRequestService notificationRequestService;

    @Scheduled(cron = "${notification.reminder.cron:0 * * * * *}")
    public void sendDueReminders() {
        sendDueReminders(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    void sendDueReminders(LocalDateTime now) {
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        sendMealReminders(time, date);
        sendExerciseReminders(now.getDayOfWeek(), time, date);
    }

    private void sendMealReminders(LocalTime time, LocalDate date) {
        requestMealReminders(MealType.BREAKFAST, findMealSettings(MealType.BREAKFAST, time), date);
        requestMealReminders(MealType.LUNCH, findMealSettings(MealType.LUNCH, time), date);
        requestMealReminders(MealType.DINNER, findMealSettings(MealType.DINNER, time), date);
    }

    private List<NotificationSetting> findMealSettings(MealType mealType, LocalTime time) {
        return switch (mealType) {
            case BREAKFAST ->
                notificationSettingRepository.findByMealReminderEnabledTrueAndBreakfastTimeAndDeletedAtIsNull(time);
            case LUNCH ->
                notificationSettingRepository.findByMealReminderEnabledTrueAndLunchTimeAndDeletedAtIsNull(time);
            case DINNER ->
                notificationSettingRepository.findByMealReminderEnabledTrueAndDinnerTimeAndDeletedAtIsNull(time);
        };
    }

    private void requestMealReminders(
        MealType mealType,
        List<NotificationSetting> notificationSettings,
        LocalDate date
    ) {
        notificationSettings.forEach(setting -> notificationRequestService.requestMealReminder(
            setting.getMemberId(),
            mealType.name(),
            date.toString()
        ));
    }

    private void sendExerciseReminders(DayOfWeek dayOfWeek, LocalTime time, LocalDate date) {
        exerciseScheduleRepository.findByDayOfWeekAndScheduledTimeAndDeletedAtIsNull(dayOfWeek, time)
            .stream()
            .filter(schedule -> isExerciseReminderEnabled(schedule.getMemberId()))
            .forEach(schedule -> requestExerciseReminder(schedule, date));
    }

    private boolean isExerciseReminderEnabled(Long memberId) {
        return notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .map(NotificationSetting::isExerciseReminderEnabled)
            .orElse(true);
    }

    private void requestExerciseReminder(ExerciseSchedule schedule, LocalDate date) {
        notificationRequestService.requestExerciseReminder(
            schedule.getMemberId(),
            schedule.getScheduledTime().toString(),
            date.toString()
        );
    }
}
