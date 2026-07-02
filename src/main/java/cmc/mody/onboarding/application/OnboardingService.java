package cmc.mody.onboarding.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.WeightRecord;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.WeightRecordRepository;
import cmc.mody.notification.domain.ExerciseSchedule;
import cmc.mody.notification.domain.MealType;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.infrastructure.repository.ExerciseScheduleRepository;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final ExerciseScheduleRepository exerciseScheduleRepository;

    @Transactional
    public ProfileSetupResult setupProfile(Long memberId, ProfileSetupCommand command) {
        Member member = getMember(memberId);

        if (member.isPersonalInfoCompleted()) {
            throw new GeneralException(ErrorStatus.MEMBER_PROFILE_ALREADY_COMPLETED);
        }

        member.completeProfile(command.nickname(), command.birthDate(), command.targetWeightKg());
        WeightRecord weightRecord = saveWeightRecord(memberId, command.currentWeightKg());
        saveNotificationSetting(
            memberId,
            hasMealReminder(command.mealSchedules()),
            command.mealSchedules(),
            true,
            null
        );
        saveExerciseSchedules(memberId, command.exerciseSchedules());

        return new ProfileSetupResult(member.getId(), weightRecord.getId(), true);
    }

    @Transactional
    public WeightSetupResult setupWeight(Long memberId, WeightSetupCommand command) {
        Member member = getMember(memberId);
        member.updateTargetWeight(command.targetWeightKg());
        WeightRecord weightRecord = saveWeightRecord(memberId, command.currentWeightKg());
        return WeightSetupResult.from(weightRecord);
    }

    @Transactional
    public NotificationSetupResult setupNotifications(Long memberId, NotificationSetupCommand command) {
        getMember(memberId);
        NotificationSetting notificationSetting = saveNotificationSetting(
            memberId,
            command.mealReminderEnabled(),
            command.mealSchedules(),
            command.exerciseReminderEnabled(),
            command.exerciseReminderTime()
        );
        return new NotificationSetupResult(notificationSetting.getId(), true);
    }

    @Transactional
    public HealthConnectionResult updateHealthConnection(Long memberId, HealthConnectionCommand command) {
        Member member = getMember(memberId);
        member.updateHealthConnection(command.connected());
        return new HealthConnectionResult(command.connected());
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private WeightRecord saveWeightRecord(Long memberId, BigDecimal currentWeightKg) {
        BigDecimal changeFromPreviousKg = weightRecordRepository
            .findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(memberId)
            .map(previous -> currentWeightKg.subtract(previous.getWeightKg()))
            .orElse(BigDecimal.ZERO);

        return weightRecordRepository.save(new WeightRecord(
            idGenerator.nextId(),
            memberId,
            LocalDate.now(),
            currentWeightKg,
            changeFromPreviousKg
        ));
    }

    private NotificationSetting saveNotificationSetting(
        Long memberId,
        boolean mealReminderEnabled,
        List<MealScheduleCommand> mealSchedules,
        boolean exerciseReminderEnabled,
        LocalTime exerciseReminderTime
    ) {
        LocalTime breakfastTime = findMealTime(mealSchedules, MealType.BREAKFAST);
        LocalTime lunchTime = findMealTime(mealSchedules, MealType.LUNCH);
        LocalTime dinnerTime = findMealTime(mealSchedules, MealType.DINNER);

        NotificationSetting notificationSetting = notificationSettingRepository
            .findByMemberIdAndDeletedAtIsNull(memberId)
            .orElseGet(() -> new NotificationSetting(idGenerator.nextId(), memberId));
        notificationSetting.updateMealAndExercise(
            mealReminderEnabled,
            breakfastTime,
            lunchTime,
            dinnerTime,
            exerciseReminderEnabled,
            exerciseReminderTime
        );
        return notificationSettingRepository.save(notificationSetting);
    }

    private LocalTime findMealTime(List<MealScheduleCommand> mealSchedules, MealType mealType) {
        return mealSchedules.stream()
            .filter(schedule -> schedule.mealType() == mealType)
            .findFirst()
            .filter(schedule -> !schedule.skipped())
            .map(MealScheduleCommand::time)
            .orElse(null);
    }

    private boolean hasMealReminder(List<MealScheduleCommand> mealSchedules) {
        return mealSchedules.stream()
            .anyMatch(schedule -> !schedule.skipped() && schedule.time() != null);
    }

    private void saveExerciseSchedules(Long memberId, List<ExerciseScheduleCommand> exerciseSchedules) {
        exerciseSchedules.stream()
            .map(schedule -> new ExerciseSchedule(
                idGenerator.nextId(),
                memberId,
                schedule.dayOfWeek(),
                schedule.time()
            ))
            .forEach(exerciseScheduleRepository::save);
    }

    public record ProfileSetupCommand(
        String nickname,
        LocalDate birthDate,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg,
        List<MealScheduleCommand> mealSchedules,
        List<ExerciseScheduleCommand> exerciseSchedules
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

    public record WeightSetupCommand(
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg
    ) {
    }

    public record NotificationSetupCommand(
        boolean mealReminderEnabled,
        List<MealScheduleCommand> mealSchedules,
        boolean exerciseReminderEnabled,
        LocalTime exerciseReminderTime
    ) {
    }

    public record HealthConnectionCommand(boolean connected) {
    }

    public record ProfileSetupResult(
        Long memberId,
        Long weightRecordId,
        boolean personalInfoCompleted
    ) {
    }

    public record WeightSetupResult(
        Long weightRecordId,
        LocalDate recordedOn,
        BigDecimal weightKg,
        BigDecimal changeFromPreviousKg
    ) {
        public static WeightSetupResult from(WeightRecord weightRecord) {
            return new WeightSetupResult(
                weightRecord.getId(),
                weightRecord.getRecordedOn(),
                weightRecord.getWeightKg(),
                weightRecord.getChangeFromPreviousKg()
            );
        }
    }

    public record NotificationSetupResult(Long notificationSettingId, boolean enabled) {
    }

    public record HealthConnectionResult(boolean connected) {
    }
}
