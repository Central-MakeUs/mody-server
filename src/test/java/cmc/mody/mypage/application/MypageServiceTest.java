package cmc.mody.mypage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.auth.domain.RefreshToken;
import cmc.mody.auth.infrastructure.repository.RefreshTokenRepository;
import cmc.mody.challenge.domain.ChallengeProof;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.StepRecord;
import cmc.mody.challenge.domain.StepSource;
import cmc.mody.challenge.infrastructure.repository.ChallengeProofRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.StepRecordRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.domain.Status;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.LoginType;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.SocialAccount;
import cmc.mody.member.domain.WeightRecord;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.member.infrastructure.repository.SocialAccountRepository;
import cmc.mody.member.infrastructure.repository.WeightRecordRepository;
import cmc.mody.mypage.application.MypageService.ProfileResult;
import cmc.mody.mypage.application.MypageService.ProfileUpdateCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateResult;
import cmc.mody.notification.application.NotificationPreferenceService;
import cmc.mody.notification.domain.ExerciseSchedule;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.MealType;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.domain.PushPlatform;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MypageServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private WeightRecordRepository weightRecordRepository;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private ExerciseScheduleRepository exerciseScheduleRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberPushTokenRepository memberPushTokenRepository;

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private ActivityRecordGroupRepository activityRecordGroupRepository;

    @Mock
    private RecordCommentRepository recordCommentRepository;

    @Mock
    private RecordViewHistoryRepository recordViewHistoryRepository;

    @Mock
    private GroupChallengeRepository groupChallengeRepository;

    @Mock
    private ChallengeProofRepository challengeProofRepository;

    @Mock
    private StepRecordRepository stepRecordRepository;

    @Captor
    private ArgumentCaptor<WeightRecord> weightRecordCaptor;

    @Test
    @DisplayName("내 정보 조회 시 자동 로그인 진입 상태를 함께 반환한다.")
    void getMyInfo() {
        MypageService service = service();
        Member member = member();
        member.completeGroupOnboarding();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            GroupMemberStatus.JOINED
        )).willReturn(1L);

        MypageService.MyInfoResult result = service.getMyInfo(1L);

        assertThat(result.personalInfoCompleted()).isTrue();
        assertThat(result.groupOnboardingCompleted()).isTrue();
        assertThat(result.mainAccessible()).isTrue();
    }

    @Test
    @DisplayName("그룹 온보딩 이력이 있어도 현재 참여 그룹이 없으면 메인에 진입할 수 없다.")
    void getMyInfoWithoutJoinedGroup() {
        MypageService service = service();
        Member member = member();
        member.completeGroupOnboarding();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            GroupMemberStatus.JOINED
        )).willReturn(0L);

        MypageService.MyInfoResult result = service.getMyInfo(1L);

        assertThat(result.personalInfoCompleted()).isTrue();
        assertThat(result.groupOnboardingCompleted()).isTrue();
        assertThat(result.mainAccessible()).isFalse();
    }

    @Test
    @DisplayName("프로필 조회 시 회원과 소셜 로그인 타입을 반환한다.")
    void getProfile() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(socialAccountRepository.findFirstByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L))
            .willReturn(Optional.of(new SocialAccount(10L, 1L, LoginType.KAKAO, "provider-id")));

        ProfileResult result = service.getProfile(1L);

        assertThat(result.loginType()).isEqualTo("KAKAO");
        assertThat(result.name()).isEqualTo("민석");
        assertThat(result.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    @DisplayName("소셜 계정이 없으면 프로필을 조회할 수 없다.")
    void getProfileWithoutSocialAccount() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(socialAccountRepository.findFirstByMemberIdAndDeletedAtIsNullOrderByCreatedAtAsc(1L))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfile(1L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.MYPAGE_SOCIAL_ACCOUNT_NOT_FOUND));
    }

    @Test
    @DisplayName("프로필 수정 시 닉네임과 생년월일을 변경한다.")
    void updateProfile() {
        MypageService service = service();
        Member member = member();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        service.updateProfile(1L, new ProfileUpdateCommand("수정", LocalDate.of(1999, 12, 31)));

        assertThat(member.getNickname()).isEqualTo("수정");
        assertThat(member.getBirthDate()).isEqualTo(LocalDate.of(1999, 12, 31));
    }

    @Test
    @DisplayName("체중 조회 시 시작, 현재, 목표 체중을 반환한다.")
    void getWeightHistory() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnAscCreatedAtAsc(1L))
            .willReturn(Optional.of(
                new WeightRecord(10L, 1L, LocalDate.of(2026, 6, 27), new BigDecimal("73.00"), BigDecimal.ZERO)
            ));
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(Optional.of(
                new WeightRecord(11L, 1L, LocalDate.of(2026, 6, 28), new BigDecimal("72.50"), new BigDecimal("-0.50"))
            ));

        MypageService.WeightHistoryResult result = service.getWeightHistory(1L);

        assertThat(result.startWeightKg()).isEqualByComparingTo("73.00");
        assertThat(result.currentWeightKg()).isEqualByComparingTo("72.50");
        assertThat(result.targetWeightKg()).isEqualByComparingTo("68.0");
    }

    @Test
    @DisplayName("체중 기록이 없으면 시작 체중과 현재 체중은 비어있다.")
    void getWeightHistoryWithoutWeightRecords() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnAscCreatedAtAsc(1L))
            .willReturn(Optional.empty());
        given(weightRecordRepository.findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(1L))
            .willReturn(Optional.empty());

        MypageService.WeightHistoryResult result = service.getWeightHistory(1L);

        assertThat(result.startWeightKg()).isNull();
        assertThat(result.currentWeightKg()).isNull();
        assertThat(result.targetWeightKg()).isEqualByComparingTo("68.0");
    }

    @Test
    @DisplayName("체중 추가 시 이전 기록 대비 증감을 저장한다.")
    void createWeight() {
        MypageService service = service();
        LocalDate recordedOn = LocalDate.of(2026, 6, 28);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(weightRecordRepository.findLatestOnOrBeforeRecordedOn(1L, recordedOn))
            .willReturn(Optional.of(
                new WeightRecord(10L, 1L, recordedOn.minusDays(1), new BigDecimal("73.00"), BigDecimal.ZERO)
            ));
        given(idGenerator.nextId()).willReturn(11L);
        given(weightRecordRepository.save(any(WeightRecord.class))).willAnswer(invocation -> invocation.getArgument(0));

        WeightCreateResult result = service.createWeight(
            1L,
            new WeightCreateCommand(recordedOn, new BigDecimal("72.50"))
        );

        assertThat(result.recordedOn()).isEqualTo(recordedOn);
        assertThat(result.changeFromPreviousKg()).isEqualByComparingTo(new BigDecimal("-0.50"));
        then(weightRecordRepository).should().save(weightRecordCaptor.capture());
        assertThat(weightRecordCaptor.getValue().getRecordedOn()).isEqualTo(recordedOn);
        assertThat(weightRecordCaptor.getValue().getChangeFromPreviousKg()).isEqualByComparingTo("-0.50");
    }

    @Test
    @DisplayName("이전 체중 기록이 없으면 증감 값은 0이다.")
    void createWeightWithoutPreviousRecord() {
        MypageService service = service();
        LocalDate recordedOn = LocalDate.of(2026, 6, 28);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(weightRecordRepository.findLatestOnOrBeforeRecordedOn(1L, recordedOn))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(10L);
        given(weightRecordRepository.save(any(WeightRecord.class))).willAnswer(invocation -> invocation.getArgument(0));

        WeightCreateResult result = service.createWeight(
            1L,
            new WeightCreateCommand(recordedOn, new BigDecimal("72.50"))
        );

        assertThat(result.changeFromPreviousKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("알림 설정 조회 시 회원 검증 후 설정을 반환한다.")
    void getNotificationSettings() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(notificationPreferenceService.getPreferences(1L)).willReturn(preferences());

        MypageService.NotificationSettingResult result = service.getNotificationSettings(1L);

        assertThat(result.recordReminderEnabled()).isTrue();
        assertThat(result.mealSchedules()).hasSize(3);
        assertThat(result.exerciseSchedules()).hasSize(3);
    }

    @Test
    @DisplayName("알림 on/off 수정 시 공통 설정 서비스에 위임한다.")
    void updateNotificationSettings() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(notificationPreferenceService.updateReminderFlags(
            eq(1L),
            any(NotificationPreferenceService.ReminderFlagCommand.class)
        )).willReturn(preferences());

        service.updateNotificationSettings(
            1L,
            new MypageService.NotificationSettingCommand(true, false, true)
        );

        then(notificationPreferenceService).should().updateReminderFlags(
            eq(1L),
            any(NotificationPreferenceService.ReminderFlagCommand.class)
        );
    }

    @Test
    @DisplayName("식사 시간 수정 시 공통 설정 서비스에 위임한다.")
    void updateMealTimes() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(notificationPreferenceService.updateMealTimes(
            eq(1L),
            any()
        )).willReturn(preferences());

        MypageService.MealTimeUpdateResult result = service.updateMealTimes(
            1L,
            new MypageService.MealTimeUpdateCommand(List.of(
                new MypageService.MealScheduleCommand(MealType.BREAKFAST, LocalTime.of(8, 0), false),
                new MypageService.MealScheduleCommand(MealType.LUNCH, null, true),
                new MypageService.MealScheduleCommand(MealType.DINNER, LocalTime.of(18, 0), false)
            ))
        );

        assertThat(result.mealSchedules()).hasSize(3);
    }

    @Test
    @DisplayName("운동 일정 수정 시 공통 설정 서비스에 위임한다.")
    void updateExerciseSchedules() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(notificationPreferenceService.updateExerciseSchedules(eq(1L), any()))
            .willReturn(new NotificationPreferenceService.ExerciseScheduleUpdateResult(
                preferences().exerciseSchedules()
            ));

        MypageService.ExerciseScheduleUpdateResult result = service.updateExerciseSchedules(
            1L,
            new MypageService.ExerciseScheduleUpdateCommand(List.of(
                new MypageService.ExerciseScheduleCommand(DayOfWeek.MONDAY, LocalTime.of(7, 30)),
                new MypageService.ExerciseScheduleCommand(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0)),
                new MypageService.ExerciseScheduleCommand(DayOfWeek.FRIDAY, LocalTime.of(9, 0))
            ))
        );

        assertThat(result.schedules()).hasSize(3);
    }

    @Test
    @DisplayName("회원 탈퇴 시 회원 관련 활성 데이터를 논리 삭제한다.")
    void deleteMe() {
        MypageService service = service();
        Member member = member();
        RefreshToken refreshToken = new RefreshToken(10L, 1L, "refresh-token");
        SocialAccount socialAccount = new SocialAccount(11L, 1L, LoginType.KAKAO, "provider-id");
        WeightRecord weightRecord = new WeightRecord(12L, 1L, LocalDate.now(), BigDecimal.valueOf(72.5), BigDecimal.ZERO);
        NotificationSetting notificationSetting = new NotificationSetting(13L, 1L);
        ExerciseSchedule exerciseSchedule = new ExerciseSchedule(14L, 1L, DayOfWeek.MONDAY, LocalTime.of(7, 30));
        Notification notification = new Notification(15L, 1L, NotificationType.COMMENT, "title", "content");
        MemberPushToken pushToken = new MemberPushToken(
            16L,
            1L,
            "device-1",
            PushPlatform.IOS,
            "fcm-token",
            LocalDateTime.now()
        );
        GroupMember groupMember = new GroupMember(17L, 1L, 100L, LocalDateTime.now());
        ActivityRecord record = mealRecord(18L, 1L, 100L);
        RecordComment myComment = new RecordComment(19L, 200L, 100L, 1L, "내 댓글");
        RecordComment recordComment = new RecordComment(20L, 18L, 100L, 2L, "기록 댓글");
        RecordViewHistory recordViewHistory = new RecordViewHistory(21L, 1L, 100L, 2L, LocalDateTime.now());
        ChallengeProof challengeProof = new ChallengeProof(22L, 300L, 1L, "challenge/proof.jpg", LocalDateTime.now());
        StepRecord stepRecord = new StepRecord(23L, 301L, 1L, LocalDate.now(), 1000, StepSource.HEALTH_KIT);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(refreshTokenRepository.findAllByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(refreshToken));
        given(socialAccountRepository.findAllByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(socialAccount));
        given(weightRecordRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(weightRecord));
        given(notificationSettingRepository.findAllByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(notificationSetting));
        given(exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(exerciseSchedule));
        given(notificationRepository.findByReceiverMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(notification));
        given(memberPushTokenRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(pushToken));
        given(groupMemberRepository.findByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(1L, GroupMemberStatus.JOINED))
            .willReturn(List.of(groupMember));
        given(activityRecordRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(record));
        given(recordCommentRepository.findByRecordIdInAndDeletedAtIsNull(List.of(18L))).willReturn(List.of(recordComment));
        given(recordCommentRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(myComment));
        given(recordViewHistoryRepository.findActiveByMemberId(1L)).willReturn(List.of(recordViewHistory));
        given(challengeProofRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(challengeProof));
        given(stepRecordRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(stepRecord));

        service.deleteMe(1L);

        assertThat(member.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(refreshToken.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(socialAccount.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(weightRecord.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(notificationSetting.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(exerciseSchedule.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(notification.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(pushToken.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(groupMember.getGroupMemberStatus()).isEqualTo(GroupMemberStatus.LEFT);
        assertThat(record.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(myComment.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(recordComment.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(recordViewHistory.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(challengeProof.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(stepRecord.getStatus()).isEqualTo(Status.INACTIVE);
    }

    @Test
    @DisplayName("그룹 구성원 조회 시 참여 중인 회원만 반환한다.")
    void getGroupMembers() {
        MypageService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(100L)).willReturn(Optional.of(new ModyGroup(100L, "ABCD2345", "모디")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            100L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            100L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(new GroupMember(
            20L,
            2L,
            100L,
            "도윤",
            "profiles/member-2.jpg",
            LocalDateTime.now()
        )));

        MypageService.GroupMemberListResult result = service.getGroupMembers(1L, 100L);

        assertThat(result.members()).hasSize(1);
        assertThat(result.members().get(0).nickname()).isEqualTo("도윤");
    }

    @Test
    @DisplayName("그룹 나가기 시 멤버십과 해당 그룹 안의 기록/댓글을 논리 삭제한다.")
    void leaveGroup() {
        MypageService service = service();
        GroupMember groupMember = new GroupMember(20L, 1L, 100L, LocalDateTime.now());
        ActivityRecord record = mealRecord(30L, 1L, 100L);
        ActivityRecordGroup recordGroup = new ActivityRecordGroup(40L, 30L, 100L, 1L, LocalDateTime.now());
        RecordComment myComment = new RecordComment(31L, 200L, 100L, 1L, "내 댓글");
        RecordComment recordComment = new RecordComment(32L, 30L, 100L, 2L, "기록 댓글");
        RecordViewHistory recordViewHistory = new RecordViewHistory(33L, 2L, 100L, 1L, LocalDateTime.now());
        GroupChallenge groupChallenge = new GroupChallenge(
            34L,
            100L,
            300L,
            LocalDate.now(),
            LocalDate.now().plusDays(6)
        );
        ChallengeProof challengeProof = new ChallengeProof(35L, 34L, 1L, "challenge/proof.jpg", LocalDateTime.now());
        StepRecord stepRecord = new StepRecord(36L, 34L, 1L, LocalDate.now(), 1000, StepSource.HEALTH_KIT);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(groupMemberRepository.findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            100L,
            GroupMemberStatus.JOINED
        )).willReturn(Optional.of(groupMember));
        given(activityRecordGroupRepository.findByMemberIdAndGroupIdAndDeletedAtIsNull(1L, 100L))
            .willReturn(List.of(recordGroup));
        given(recordCommentRepository.findByRecordIdInAndGroupIdAndDeletedAtIsNull(List.of(30L), 100L))
            .willReturn(List.of(recordComment));
        given(recordCommentRepository.findActiveCommentsByMemberIdAndGroupId(1L, 100L)).willReturn(List.of(myComment));
        given(recordViewHistoryRepository.findActiveByMemberIdAndGroupId(1L, 100L)).willReturn(List.of(recordViewHistory));
        given(groupChallengeRepository.findByGroupIdAndDeletedAtIsNull(100L)).willReturn(List.of(groupChallenge));
        given(challengeProofRepository.findByMemberIdAndGroupChallengeIdInAndDeletedAtIsNull(1L, List.of(34L)))
            .willReturn(List.of(challengeProof));
        given(stepRecordRepository.findByMemberIdAndGroupChallengeIdInAndDeletedAtIsNull(1L, List.of(34L)))
            .willReturn(List.of(stepRecord));

        service.leaveGroup(1L, 100L);

        assertThat(groupMember.getGroupMemberStatus()).isEqualTo(GroupMemberStatus.LEFT);
        assertThat(record.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(recordGroup.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(myComment.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(recordComment.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(recordViewHistory.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(challengeProof.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(stepRecord.getStatus()).isEqualTo(Status.INACTIVE);
    }

    private MypageService service() {
        return new MypageService(
            idGenerator,
            memberRepository,
            socialAccountRepository,
            weightRecordRepository,
            notificationPreferenceService,
            refreshTokenRepository,
            groupMemberRepository,
            modyGroupRepository,
            notificationSettingRepository,
            exerciseScheduleRepository,
            notificationRepository,
            memberPushTokenRepository,
            activityRecordRepository,
            activityRecordGroupRepository,
            recordCommentRepository,
            recordViewHistoryRepository,
            groupChallengeRepository,
            challengeProofRepository,
            stepRecordRepository
        );
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }

    private NotificationPreferenceService.NotificationPreferenceResult preferences() {
        return new NotificationPreferenceService.NotificationPreferenceResult(
            true,
            true,
            true,
            List.of(
                new NotificationPreferenceService.MealScheduleResult(
                    MealType.BREAKFAST,
                    LocalTime.of(8, 0),
                    false
                ),
                new NotificationPreferenceService.MealScheduleResult(MealType.LUNCH, null, true),
                new NotificationPreferenceService.MealScheduleResult(
                    MealType.DINNER,
                    LocalTime.of(18, 0),
                    false
                )
            ),
            List.of(
                new NotificationPreferenceService.ExerciseScheduleResult(DayOfWeek.MONDAY, LocalTime.of(7, 30)),
                new NotificationPreferenceService.ExerciseScheduleResult(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0)),
                new NotificationPreferenceService.ExerciseScheduleResult(DayOfWeek.FRIDAY, LocalTime.of(9, 0))
            )
        );
    }

    private ActivityRecord mealRecord(Long id, Long memberId, Long groupId) {
        return ActivityRecord.meal(
            id,
            memberId,
            groupId,
            LocalTime.of(8, 0),
            "샐러드",
            "records/meal.jpg",
            LocalDateTime.now()
        );
    }
}
