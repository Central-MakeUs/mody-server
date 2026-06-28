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
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        if (member.isPersonalInfoCompleted()) {
            throw new GeneralException(ErrorStatus.MEMBER_PROFILE_ALREADY_COMPLETED);
        }

        member.completeProfile(command.nickname(), command.birthDate(), command.targetWeightKg());
        WeightRecord weightRecord = saveWeightRecord(memberId, command.currentWeightKg());
        saveNotificationSetting(memberId, command.mealSchedules());
        saveExerciseSchedules(memberId, command.exerciseSchedules());

        return new ProfileSetupResult(member.getId(), weightRecord.getId(), true);
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

    private void saveNotificationSetting(
        Long memberId,
        List<MealScheduleCommand> mealSchedules
    ) {
        LocalTime breakfastTime = findMealTime(mealSchedules, MealType.BREAKFAST);
        LocalTime lunchTime = findMealTime(mealSchedules, MealType.LUNCH);
        LocalTime dinnerTime = findMealTime(mealSchedules, MealType.DINNER);
        boolean mealReminderEnabled = breakfastTime != null || lunchTime != null || dinnerTime != null;

        notificationSettingRepository.save(new NotificationSetting(
            idGenerator.nextId(),
            memberId,
            mealReminderEnabled,
            breakfastTime,
            lunchTime,
            dinnerTime,
            true,
            null
        ));
    }

    private LocalTime findMealTime(List<MealScheduleCommand> mealSchedules, MealType mealType) {
        return mealSchedules.stream()
            .filter(schedule -> schedule.mealType() == mealType)
            .findFirst()
            .filter(schedule -> !schedule.skipped())
            .map(MealScheduleCommand::time)
            .orElse(null);
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

    public record ProfileSetupResult(
        Long memberId,
        Long weightRecordId,
        boolean personalInfoCompleted
    ) {
    }
}
