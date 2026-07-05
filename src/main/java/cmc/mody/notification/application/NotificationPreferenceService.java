package cmc.mody.notification.application;

import cmc.mody.common.id.IdGenerator;
import cmc.mody.notification.domain.ExerciseSchedule;
import cmc.mody.notification.domain.MealType;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.infrastructure.repository.ExerciseScheduleRepository;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {
    private final IdGenerator idGenerator;
    private final NotificationSettingRepository notificationSettingRepository;
    private final ExerciseScheduleRepository exerciseScheduleRepository;

    public NotificationPreferenceResult getPreferences(Long memberId) {
        NotificationSetting notificationSetting = notificationSettingRepository
            .findByMemberIdAndDeletedAtIsNull(memberId)
            .orElse(null);
        List<ExerciseScheduleResult> exerciseSchedules = findExerciseSchedules(memberId);

        if (notificationSetting == null) {
            return NotificationPreferenceResult.defaults(exerciseSchedules);
        }
        return NotificationPreferenceResult.from(notificationSetting, exerciseSchedules);
    }

    public NotificationPreferenceResult updateReminderFlags(Long memberId, ReminderFlagCommand command) {
        NotificationSetting notificationSetting = getOrCreateNotificationSetting(memberId);
        notificationSetting.updateReminderFlags(
            command.recordReminderEnabled(),
            command.recordReminderEnabled(),
            command.commentNotificationEnabled(),
            command.challengeNotificationEnabled()
        );
        NotificationSetting saved = notificationSettingRepository.save(notificationSetting);
        return NotificationPreferenceResult.from(saved, findExerciseSchedules(memberId));
    }

    public NotificationSetting saveInitialReminderFlags(Long memberId, ReminderFlagCommand command) {
        NotificationSetting notificationSetting = getOrCreateNotificationSetting(memberId);
        notificationSetting.updateReminderFlags(
            command.recordReminderEnabled(),
            command.recordReminderEnabled(),
            command.commentNotificationEnabled(),
            command.challengeNotificationEnabled()
        );
        return notificationSettingRepository.save(notificationSetting);
    }

    public NotificationPreferenceResult updateMealTimes(Long memberId, List<MealScheduleCommand> mealSchedules) {
        NotificationSetting notificationSetting = getOrCreateNotificationSetting(memberId);
        notificationSetting.updateMealTimes(
            findMealTime(mealSchedules, MealType.BREAKFAST),
            findMealTime(mealSchedules, MealType.LUNCH),
            findMealTime(mealSchedules, MealType.DINNER)
        );
        NotificationSetting saved = notificationSettingRepository.save(notificationSetting);
        return NotificationPreferenceResult.from(saved, findExerciseSchedules(memberId));
    }

    public ExerciseScheduleUpdateResult updateExerciseSchedules(
        Long memberId,
        List<ExerciseScheduleCommand> exerciseSchedules
    ) {
        exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(ExerciseSchedule::delete);

        List<ExerciseScheduleResult> savedSchedules = exerciseSchedules.stream()
            .map(schedule -> exerciseScheduleRepository.save(new ExerciseSchedule(
                idGenerator.nextId(),
                memberId,
                schedule.dayOfWeek(),
                schedule.time()
            )))
            .map(ExerciseScheduleResult::from)
            .sorted(Comparator
                .comparing((ExerciseScheduleResult schedule) -> schedule.dayOfWeek().getValue())
                .thenComparing(ExerciseScheduleResult::time))
            .toList();

        return new ExerciseScheduleUpdateResult(savedSchedules);
    }

    public NotificationSetting saveInitialNotificationSetting(
        Long memberId,
        boolean mealReminderEnabled,
        List<MealScheduleCommand> mealSchedules,
        boolean exerciseReminderEnabled,
        LocalTime exerciseReminderTime
    ) {
        NotificationSetting notificationSetting = getOrCreateNotificationSetting(memberId);
        notificationSetting.updateMealAndExercise(
            mealReminderEnabled,
            findMealTime(mealSchedules, MealType.BREAKFAST),
            findMealTime(mealSchedules, MealType.LUNCH),
            findMealTime(mealSchedules, MealType.DINNER),
            exerciseReminderEnabled,
            exerciseReminderTime
        );
        return notificationSettingRepository.save(notificationSetting);
    }

    public ExerciseScheduleUpdateResult saveInitialExerciseSchedules(
        Long memberId,
        List<ExerciseScheduleCommand> exerciseSchedules
    ) {
        return updateExerciseSchedules(memberId, exerciseSchedules);
    }

    private NotificationSetting getOrCreateNotificationSetting(Long memberId) {
        return notificationSettingRepository
            .findByMemberIdAndDeletedAtIsNull(memberId)
            .orElseGet(() -> new NotificationSetting(idGenerator.nextId(), memberId));
    }

    private LocalTime findMealTime(List<MealScheduleCommand> mealSchedules, MealType mealType) {
        return mealSchedules.stream()
            .filter(schedule -> schedule.mealType() == mealType)
            .findFirst()
            .filter(schedule -> !schedule.skipped())
            .map(MealScheduleCommand::time)
            .orElse(null);
    }

    private List<ExerciseScheduleResult> findExerciseSchedules(Long memberId) {
        return exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(memberId).stream()
            .map(ExerciseScheduleResult::from)
            .sorted(Comparator
                .comparing((ExerciseScheduleResult schedule) -> schedule.dayOfWeek().getValue())
                .thenComparing(ExerciseScheduleResult::time))
            .toList();
    }

    public record ReminderFlagCommand(
        boolean recordReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled
    ) {
    }

    public record MealScheduleCommand(
        MealType mealType,
        LocalTime time,
        boolean skipped
    ) {
    }

    public record ExerciseScheduleCommand(
        DayOfWeek dayOfWeek,
        LocalTime time
    ) {
    }

    public record NotificationPreferenceResult(
        boolean recordReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<MealScheduleResult> mealSchedules,
        List<ExerciseScheduleResult> exerciseSchedules
    ) {
        public static NotificationPreferenceResult defaults(List<ExerciseScheduleResult> exerciseSchedules) {
            return new NotificationPreferenceResult(
                true,
                true,
                true,
                List.of(
                    new MealScheduleResult(MealType.BREAKFAST, null, true),
                    new MealScheduleResult(MealType.LUNCH, null, true),
                    new MealScheduleResult(MealType.DINNER, null, true)
                ),
                exerciseSchedules
            );
        }

        public static NotificationPreferenceResult from(
            NotificationSetting notificationSetting,
            List<ExerciseScheduleResult> exerciseSchedules
        ) {
            return new NotificationPreferenceResult(
                notificationSetting.isMealReminderEnabled() || notificationSetting.isExerciseReminderEnabled(),
                notificationSetting.isCommentNotificationEnabled(),
                notificationSetting.isChallengeNotificationEnabled(),
                List.of(
                    MealScheduleResult.from(MealType.BREAKFAST, notificationSetting.getBreakfastTime()),
                    MealScheduleResult.from(MealType.LUNCH, notificationSetting.getLunchTime()),
                    MealScheduleResult.from(MealType.DINNER, notificationSetting.getDinnerTime())
                ),
                exerciseSchedules
            );
        }
    }

    public record MealScheduleResult(
        MealType mealType,
        LocalTime time,
        boolean skipped
    ) {
        public static MealScheduleResult from(MealType mealType, LocalTime time) {
            return new MealScheduleResult(mealType, time, time == null);
        }
    }

    public record ExerciseScheduleResult(
        DayOfWeek dayOfWeek,
        LocalTime time
    ) {
        public static ExerciseScheduleResult from(ExerciseSchedule exerciseSchedule) {
            return new ExerciseScheduleResult(
                exerciseSchedule.getDayOfWeek(),
                exerciseSchedule.getScheduledTime()
            );
        }
    }

    public record ExerciseScheduleUpdateResult(
        List<ExerciseScheduleResult> schedules
    ) {
    }
}
