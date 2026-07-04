package cmc.mody.mypage.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.SocialAccount;
import cmc.mody.member.domain.WeightRecord;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.SocialAccountRepository;
import cmc.mody.member.infrastructure.repository.WeightRecordRepository;
import cmc.mody.notification.application.NotificationPreferenceService;
import cmc.mody.notification.domain.MealType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MypageService {
    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final NotificationPreferenceService notificationPreferenceService;

    @Transactional(readOnly = true)
    public MyInfoResult getMyInfo(Long memberId) {
        Member member = getMember(memberId);
        return new MyInfoResult(
            member.getId(),
            member.getNickname(),
            member.getProfileImageKey(),
            calculateDaysTogether(member)
        );
    }

    @Transactional(readOnly = true)
    public ProfileResult getProfile(Long memberId) {
        Member member = getMember(memberId);
        SocialAccount socialAccount = socialAccountRepository
            .findFirstByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(memberId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MYPAGE_SOCIAL_ACCOUNT_NOT_FOUND));
        return new ProfileResult(
            socialAccount.getLoginType().name(),
            member.getNickname(),
            member.getBirthDate()
        );
    }

    @Transactional
    public ProfileUpdateResult updateProfile(Long memberId, ProfileUpdateCommand command) {
        Member member = getMember(memberId);
        member.updateProfile(command.nickname(), command.birthDate());
        return new ProfileUpdateResult(member.getNickname(), member.getBirthDate());
    }

    @Transactional(readOnly = true)
    public WeightHistoryResult getWeightHistory(Long memberId) {
        getMember(memberId);
        List<WeightRecordResult> weights = weightRecordRepository
            .findByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(memberId)
            .stream()
            .map(WeightRecordResult::from)
            .toList();
        return new WeightHistoryResult(weights);
    }

    @Transactional
    public WeightCreateResult createWeight(Long memberId, WeightCreateCommand command) {
        getMember(memberId);
        BigDecimal changeFromPreviousKg = weightRecordRepository
            .findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(memberId)
            .map(previous -> command.weightKg().subtract(previous.getWeightKg()))
            .orElse(BigDecimal.ZERO);

        WeightRecord weightRecord = weightRecordRepository.save(new WeightRecord(
            idGenerator.nextId(),
            memberId,
            LocalDate.now(),
            command.weightKg(),
            changeFromPreviousKg
        ));
        return WeightCreateResult.from(weightRecord);
    }

    @Transactional(readOnly = true)
    public NotificationSettingResult getNotificationSettings(Long memberId) {
        getMember(memberId);
        return NotificationSettingResult.from(notificationPreferenceService.getPreferences(memberId));
    }

    @Transactional
    public NotificationSettingResult updateNotificationSettings(
        Long memberId,
        NotificationSettingCommand command
    ) {
        getMember(memberId);
        NotificationPreferenceService.NotificationPreferenceResult result =
            notificationPreferenceService.updateReminderFlags(
                memberId,
                new NotificationPreferenceService.ReminderFlagCommand(
                    command.mealReminderEnabled(),
                    command.exerciseReminderEnabled(),
                    command.commentNotificationEnabled(),
                    command.challengeNotificationEnabled()
                )
            );
        return NotificationSettingResult.from(result);
    }

    @Transactional
    public ExerciseScheduleUpdateResult updateExerciseSchedules(
        Long memberId,
        ExerciseScheduleUpdateCommand command
    ) {
        getMember(memberId);
        NotificationPreferenceService.ExerciseScheduleUpdateResult result =
            notificationPreferenceService.updateExerciseSchedules(
                memberId,
                command.schedules().stream()
                    .map(schedule -> new NotificationPreferenceService.ExerciseScheduleCommand(
                        schedule.dayOfWeek(),
                        schedule.time()
                    ))
                    .toList()
            );
        return ExerciseScheduleUpdateResult.from(result);
    }

    @Transactional
    public MealTimeUpdateResult updateMealTimes(Long memberId, MealTimeUpdateCommand command) {
        getMember(memberId);
        NotificationPreferenceService.NotificationPreferenceResult result =
            notificationPreferenceService.updateMealTimes(
                memberId,
                command.mealSchedules().stream()
                    .map(schedule -> new NotificationPreferenceService.MealScheduleCommand(
                        schedule.mealType(),
                        schedule.time(),
                        schedule.skipped()
                    ))
                    .toList()
            );
        return MealTimeUpdateResult.from(result);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private int calculateDaysTogether(Member member) {
        if (member.getCreatedAt() == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(member.getCreatedAt().toLocalDate(), LocalDate.now()) + 1;
        return Math.toIntExact(Math.max(days, 1));
    }

    public record MyInfoResult(
        Long memberId,
        String nickname,
        String profileImageUrl,
        int daysTogether
    ) {
    }

    public record ProfileResult(String loginType, String name, LocalDate birthDate) {
    }

    public record ProfileUpdateCommand(String nickname, LocalDate birthDate) {
    }

    public record ProfileUpdateResult(String nickname, LocalDate birthDate) {
    }

    public record WeightHistoryResult(List<WeightRecordResult> weights) {
    }

    public record WeightRecordResult(
        Long weightRecordId,
        LocalDate recordedOn,
        BigDecimal weightKg,
        BigDecimal changeFromPreviousKg
    ) {
        public static WeightRecordResult from(WeightRecord weightRecord) {
            return new WeightRecordResult(
                weightRecord.getId(),
                weightRecord.getRecordedOn(),
                weightRecord.getWeightKg(),
                weightRecord.getChangeFromPreviousKg()
            );
        }
    }

    public record WeightCreateCommand(BigDecimal weightKg) {
    }

    public record WeightCreateResult(
        Long weightRecordId,
        LocalDate recordedOn,
        BigDecimal weightKg,
        BigDecimal changeFromPreviousKg
    ) {
        public static WeightCreateResult from(WeightRecord weightRecord) {
            return new WeightCreateResult(
                weightRecord.getId(),
                weightRecord.getRecordedOn(),
                weightRecord.getWeightKg(),
                weightRecord.getChangeFromPreviousKg()
            );
        }
    }

    public record NotificationSettingCommand(
        boolean mealReminderEnabled,
        boolean exerciseReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled
    ) {
    }

    public record NotificationSettingResult(
        boolean mealReminderEnabled,
        boolean exerciseReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<MealScheduleResult> mealSchedules,
        List<ExerciseScheduleResult> exerciseSchedules
    ) {
        public static NotificationSettingResult from(
            NotificationPreferenceService.NotificationPreferenceResult result
        ) {
            return new NotificationSettingResult(
                result.mealReminderEnabled(),
                result.exerciseReminderEnabled(),
                result.commentNotificationEnabled(),
                result.challengeNotificationEnabled(),
                result.mealSchedules().stream()
                    .map(MealScheduleResult::from)
                    .toList(),
                result.exerciseSchedules().stream()
                    .map(ExerciseScheduleResult::from)
                    .toList()
            );
        }
    }

    public record MealScheduleCommand(
        MealType mealType,
        LocalTime time,
        boolean skipped
    ) {
    }

    public record MealScheduleResult(
        MealType mealType,
        LocalTime time,
        boolean skipped
    ) {
        public static MealScheduleResult from(NotificationPreferenceService.MealScheduleResult result) {
            return new MealScheduleResult(result.mealType(), result.time(), result.skipped());
        }
    }

    public record ExerciseScheduleCommand(
        DayOfWeek dayOfWeek,
        LocalTime time
    ) {
    }

    public record ExerciseScheduleResult(
        DayOfWeek dayOfWeek,
        LocalTime time
    ) {
        public static ExerciseScheduleResult from(NotificationPreferenceService.ExerciseScheduleResult result) {
            return new ExerciseScheduleResult(result.dayOfWeek(), result.time());
        }
    }

    public record ExerciseScheduleUpdateCommand(List<ExerciseScheduleCommand> schedules) {
    }

    public record ExerciseScheduleUpdateResult(List<ExerciseScheduleResult> schedules) {
        public static ExerciseScheduleUpdateResult from(
            NotificationPreferenceService.ExerciseScheduleUpdateResult result
        ) {
            return new ExerciseScheduleUpdateResult(result.schedules().stream()
                .map(ExerciseScheduleResult::from)
                .toList());
        }
    }

    public record MealTimeUpdateCommand(List<MealScheduleCommand> mealSchedules) {
    }

    public record MealTimeUpdateResult(List<MealScheduleResult> mealSchedules) {
        public static MealTimeUpdateResult from(NotificationPreferenceService.NotificationPreferenceResult result) {
            return new MealTimeUpdateResult(result.mealSchedules().stream()
                .map(MealScheduleResult::from)
                .toList());
        }
    }
}
