package cmc.mody.mypage.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.auth.domain.RefreshToken;
import cmc.mody.auth.infrastructure.repository.RefreshTokenRepository;
import cmc.mody.challenge.domain.ChallengeProof;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.StepRecord;
import cmc.mody.challenge.infrastructure.repository.ChallengeProofRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.StepRecordRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.SocialAccount;
import cmc.mody.member.domain.WeightRecord;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.SocialAccountRepository;
import cmc.mody.member.infrastructure.repository.WeightRecordRepository;
import cmc.mody.notification.application.NotificationPreferenceService;
import cmc.mody.notification.domain.ExerciseSchedule;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.MealType;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.infrastructure.repository.ExerciseScheduleRepository;
import cmc.mody.notification.infrastructure.repository.MemberPushTokenRepository;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordViewHistory;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import cmc.mody.record.infrastructure.repository.RecordViewHistoryRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final ExerciseScheduleRepository exerciseScheduleRepository;
    private final NotificationRepository notificationRepository;
    private final MemberPushTokenRepository memberPushTokenRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordGroupRepository activityRecordGroupRepository;
    private final RecordCommentRepository recordCommentRepository;
    private final RecordViewHistoryRepository recordViewHistoryRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final ChallengeProofRepository challengeProofRepository;
    private final StepRecordRepository stepRecordRepository;

    @Transactional(readOnly = true)
    public MyInfoResult getMyInfo(Long memberId) {
        Member member = getMember(memberId);
        boolean personalInfoCompleted = member.isPersonalInfoCompleted();
        boolean mainAccessible = personalInfoCompleted && hasJoinedGroup(memberId);
        return new MyInfoResult(
            member.getId(),
            member.getNickname(),
            member.getProfileImageKey(),
            calculateDaysTogether(member),
            personalInfoCompleted,
            member.isGroupOnboardingCompleted(),
            mainAccessible
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
        Member member = getMember(memberId);
        BigDecimal startWeightKg = weightRecordRepository
            .findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnAscCreatedAtAsc(memberId)
            .map(WeightRecord::getWeightKg)
            .orElse(null);
        BigDecimal currentWeightKg = weightRecordRepository
            .findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(memberId)
            .map(WeightRecord::getWeightKg)
            .orElse(null);
        return new WeightHistoryResult(startWeightKg, currentWeightKg, member.getTargetWeightKg());
    }

    @Transactional
    public WeightCreateResult createWeight(Long memberId, WeightCreateCommand command) {
        getMember(memberId);
        BigDecimal changeFromPreviousKg = weightRecordRepository
            .findLatestOnOrBeforeRecordedOn(memberId, command.recordedOn())
            .map(previous -> command.weightKg().subtract(previous.getWeightKg()))
            .orElse(BigDecimal.ZERO);

        WeightRecord weightRecord = weightRecordRepository.save(new WeightRecord(
            idGenerator.nextId(),
            memberId,
            command.recordedOn(),
            command.weightKg(),
            changeFromPreviousKg
        ));
        return WeightCreateResult.from(weightRecord);
    }

    @Transactional
    public void deleteMe(Long memberId) {
        Member member = getMember(memberId);
        deleteRefreshTokens(memberId);
        deleteSocialAccounts(memberId);
        deleteWeightRecords(memberId);
        deleteNotificationData(memberId);
        deleteJoinedGroupMemberships(memberId);
        deleteMemberRecordsAndComments(memberId);
        deleteRecordViewHistories(memberId);
        deleteChallengeData(memberId);
        member.delete();
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
                    command.recordReminderEnabled(),
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

    @Transactional(readOnly = true)
    public GroupMemberListResult getGroupMembers(Long memberId, Long groupId) {
        getMember(memberId);
        validateGroupMembership(memberId, groupId);
        List<GroupMemberResult> members = groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .map(groupMember -> new GroupMemberResult(
                groupMember.getMemberId(),
                groupMember.getDisplayNickname(),
                groupMember.getDisplayProfileImageKey()
            ))
            .toList();
        return new GroupMemberListResult(members);
    }

    @Transactional
    public void leaveGroup(Long memberId, Long groupId) {
        getMember(memberId);
        GroupMember groupMember = groupMemberRepository
            .findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(memberId, groupId, GroupMemberStatus.JOINED)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
        deleteGroupRecordsAndComments(memberId, groupId);
        deleteGroupRecordViewHistories(memberId, groupId);
        deleteGroupChallengeData(memberId, groupId);
        groupMember.leave(LocalDateTime.now());
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private void validateGroupMembership(Long memberId, Long groupId) {
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        boolean joined = groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            groupId,
            GroupMemberStatus.JOINED
        );
        if (!joined) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
    }

    private void deleteRefreshTokens(Long memberId) {
        refreshTokenRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdAsc(memberId)
            .forEach(RefreshToken::delete);
    }

    private void deleteSocialAccounts(Long memberId) {
        socialAccountRepository.findAllByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(SocialAccount::delete);
    }

    private void deleteWeightRecords(Long memberId) {
        weightRecordRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(WeightRecord::delete);
    }

    private void deleteNotificationData(Long memberId) {
        notificationSettingRepository.findAllByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(NotificationSetting::delete);
        exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(ExerciseSchedule::delete);
        notificationRepository.findByReceiverMemberIdAndDeletedAtIsNull(memberId)
            .forEach(Notification::delete);
        memberPushTokenRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(MemberPushToken::delete);
    }

    private void deleteJoinedGroupMemberships(Long memberId) {
        LocalDateTime leftAt = LocalDateTime.now();
        groupMemberRepository.findByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(memberId, GroupMemberStatus.JOINED)
            .forEach(groupMember -> groupMember.leave(leftAt));
    }

    private void deleteMemberRecordsAndComments(Long memberId) {
        List<ActivityRecord> records = activityRecordRepository.findByMemberIdAndDeletedAtIsNull(memberId);
        deleteCommentsOnRecords(records);
        deleteRecordGroups(records);
        recordCommentRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(RecordComment::delete);
        records.forEach(ActivityRecord::delete);
    }

    private void deleteRecordViewHistories(Long memberId) {
        recordViewHistoryRepository.findActiveByMemberId(memberId)
            .forEach(RecordViewHistory::delete);
    }

    private void deleteChallengeData(Long memberId) {
        challengeProofRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(ChallengeProof::delete);
        stepRecordRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .forEach(StepRecord::delete);
    }

    private void deleteGroupRecordsAndComments(Long memberId, Long groupId) {
        List<ActivityRecordGroup> recordGroups =
            activityRecordGroupRepository.findByMemberIdAndGroupIdAndDeletedAtIsNull(memberId, groupId);
        List<Long> recordIds = recordGroups.stream()
            .map(ActivityRecordGroup::getRecordId)
            .toList();
        if (!recordIds.isEmpty()) {
            recordCommentRepository.findByRecordIdInAndGroupIdAndDeletedAtIsNull(recordIds, groupId)
                .forEach(RecordComment::delete);
            recordGroups.forEach(ActivityRecordGroup::delete);
        }
        recordCommentRepository.findActiveCommentsByMemberIdAndGroupId(memberId, groupId)
            .forEach(RecordComment::delete);
    }

    private void deleteGroupRecordViewHistories(Long memberId, Long groupId) {
        recordViewHistoryRepository.findActiveByMemberIdAndGroupId(memberId, groupId)
            .forEach(RecordViewHistory::delete);
    }

    private void deleteGroupChallengeData(Long memberId, Long groupId) {
        List<Long> groupChallengeIds = groupChallengeRepository.findByGroupIdAndDeletedAtIsNull(groupId)
            .stream()
            .map(GroupChallenge::getId)
            .toList();
        if (groupChallengeIds.isEmpty()) {
            return;
        }

        challengeProofRepository.findByMemberIdAndGroupChallengeIdInAndDeletedAtIsNull(memberId, groupChallengeIds)
            .forEach(ChallengeProof::delete);
        stepRecordRepository.findByMemberIdAndGroupChallengeIdInAndDeletedAtIsNull(memberId, groupChallengeIds)
            .forEach(StepRecord::delete);
    }

    private void deleteCommentsOnRecords(List<ActivityRecord> records) {
        List<Long> recordIds = records.stream()
            .map(ActivityRecord::getId)
            .toList();
        if (recordIds.isEmpty()) {
            return;
        }
        recordCommentRepository.findByRecordIdInAndDeletedAtIsNull(recordIds)
            .forEach(RecordComment::delete);
    }

    private void deleteRecordGroups(List<ActivityRecord> records) {
        List<Long> recordIds = records.stream()
            .map(ActivityRecord::getId)
            .toList();
        if (recordIds.isEmpty()) {
            return;
        }
        activityRecordGroupRepository.findByRecordIdInAndDeletedAtIsNull(recordIds)
            .forEach(ActivityRecordGroup::delete);
    }

    private int calculateDaysTogether(Member member) {
        if (member.getCreatedAt() == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(member.getCreatedAt().toLocalDate(), LocalDate.now()) + 1;
        return Math.toIntExact(Math.max(days, 1));
    }

    private boolean hasJoinedGroup(Long memberId) {
        return groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            GroupMemberStatus.JOINED
        ) > 0;
    }

    public record MyInfoResult(
        Long memberId,
        String nickname,
        String profileImageUrl,
        int daysTogether,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted,
        boolean mainAccessible
    ) {
    }

    public record ProfileResult(String loginType, String name, LocalDate birthDate) {
    }

    public record ProfileUpdateCommand(String nickname, LocalDate birthDate) {
    }

    public record ProfileUpdateResult(String nickname, LocalDate birthDate) {
    }

    public record WeightHistoryResult(
        BigDecimal startWeightKg,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg
    ) {
    }

    public record WeightCreateCommand(LocalDate recordedOn, BigDecimal weightKg) {
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
        boolean recordReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled
    ) {
    }

    public record NotificationSettingResult(
        boolean recordReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<MealScheduleResult> mealSchedules,
        List<ExerciseScheduleResult> exerciseSchedules
    ) {
        public static NotificationSettingResult from(
            NotificationPreferenceService.NotificationPreferenceResult result
        ) {
            return new NotificationSettingResult(
                result.recordReminderEnabled(),
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

    public record GroupMemberListResult(List<GroupMemberResult> members) {
    }

    public record GroupMemberResult(Long memberId, String nickname, String profileImageUrl) {
    }
}
