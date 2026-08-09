package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.challenge.application.ChallengeHomeService.ChallengeSummaryResult;
import cmc.mody.challenge.application.ChallengeHomeService.NudgeButtonStatus;
import cmc.mody.challenge.application.ChallengeHomeService.NudgeTargetListResult;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.upload.ImageUrlResolver;
import cmc.mody.common.upload.UploadProperties;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationRequestService;
import cmc.mody.notification.application.BuddyNudgeDedupeKey;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChallengeHomeServiceTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private GroupChallengeRepository groupChallengeRepository;

    @Mock
    private NotificationRequestService notificationRequestService;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("챌린지 홈 요약은 가입일과 이번 달 그룹 활동 통계를 반환한다.")
    void getChallengeSummary() {
        ChallengeHomeService service = service();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime firstDay = today.minusDays(1).atTime(9, 0);
        LocalDateTime secondDay = today.atTime(9, 0);
        GroupMember currentMember = groupMember(1L, "민석", today.minusDays(3).atTime(10, 0));
        GroupMember buddy = groupMember(2L, "친구", today.minusDays(2).atTime(10, 0));
        givenValidGroupMembership(1L, currentMember);
        givenJoinedMembers(List.of(currentMember, buddy));
        givenGroupMemberHistory(List.of(currentMember, buddy));
        List<ActivityRecord> records = List.of(
            mealRecord(1L, firstDay),
            exerciseRecord(1L, firstDay.plusHours(1), 30),
            mealRecord(2L, firstDay.plusHours(2)),
            exerciseRecord(1L, secondDay, 40)
        );
        given(activityRecordRepository.findActiveGroupRecordsBetween(any(), any(), any(), any()))
            .willReturn(records);
        given(activityRecordRepository.findGroupRecordsBetween(any(), any(), any())).willReturn(records);
        given(groupChallengeRepository
            .countByGroupIdAndGroupChallengeStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanAndDeletedAtIsNull(
                10L,
                GroupChallengeStatus.COMPLETED,
                monthStart.atStartOfDay(),
                monthStart.plusMonths(1).atStartOfDay()
            ))
            .willReturn(2L);

        ChallengeSummaryResult result = service.getChallengeSummary(1L, 10L);

        assertThat(result.daysTogether()).isEqualTo(4);
        assertThat(result.allMemberRecordedDays()).isEqualTo(1);
        assertThat(result.hasStartedStreak()).isTrue();
        assertThat(result.monthlyExerciseMinutes()).isEqualTo(70);
        assertThat(result.monthlyCompletedChallengeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("전원 기록일이 없으면 연속 기록을 시작하지 않은 것으로 반환한다.")
    void getChallengeSummaryWithoutStartedStreak() {
        ChallengeHomeService service = service();
        LocalDate today = LocalDate.now();
        GroupMember currentMember = groupMember(1L, "민석", today.minusDays(3).atTime(10, 0));
        GroupMember buddy = groupMember(2L, "친구", today.minusDays(2).atTime(10, 0));
        givenValidGroupMembership(1L, currentMember);
        givenJoinedMembers(List.of(currentMember, buddy));
        givenGroupMemberHistory(List.of(currentMember, buddy));
        given(activityRecordRepository.findActiveGroupRecordsBetween(any(), any(), any(), any()))
            .willReturn(List.of(mealRecord(1L, today.atTime(9, 0))));
        given(activityRecordRepository.findGroupRecordsBetween(any(), any(), any()))
            .willReturn(List.of(mealRecord(1L, today.atTime(9, 0))));
        given(groupChallengeRepository
            .countByGroupIdAndGroupChallengeStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanAndDeletedAtIsNull(
                any(),
                any(),
                any(),
                any()
            ))
            .willReturn(0L);

        ChallengeSummaryResult result = service.getChallengeSummary(1L, 10L);

        assertThat(result.hasStartedStreak()).isFalse();
    }

    @Test
    @DisplayName("새 구성원이 합류한 뒤에도 이전 구성원 전원이 기록한 이력이 있으면 연속 기록을 시작한 것으로 반환한다.")
    void getChallengeSummaryKeepsStartedStreakAfterNewMemberJoined() {
        ChallengeHomeService service = service();
        LocalDate today = LocalDate.now();
        LocalDate recordedDate = today.minusDays(5);
        GroupMember currentMember = groupMember(1L, "민석", today.minusDays(10).atTime(10, 0));
        GroupMember buddy = groupMember(2L, "친구", today.minusDays(10).atTime(10, 0));
        GroupMember newMember = groupMember(3L, "새친구", today.minusDays(1).atTime(10, 0));
        List<ActivityRecord> startedStreakRecords = List.of(
            mealRecord(1L, recordedDate.atTime(9, 0)),
            mealRecord(2L, recordedDate.atTime(10, 0))
        );
        givenValidGroupMembership(1L, currentMember);
        givenJoinedMembers(List.of(currentMember, buddy, newMember));
        givenGroupMemberHistory(List.of(currentMember, buddy, newMember));
        given(activityRecordRepository.findActiveGroupRecordsBetween(any(), any(), any(), any()))
            .willReturn(List.of());
        given(activityRecordRepository.findGroupRecordsBetween(any(), any(), any())).willReturn(startedStreakRecords);
        given(groupChallengeRepository
            .countByGroupIdAndGroupChallengeStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanAndDeletedAtIsNull(
                any(),
                any(),
                any(),
                any()
            ))
            .willReturn(0L);

        ChallengeSummaryResult result = service.getChallengeSummary(1L, 10L);

        assertThat(result.hasStartedStreak()).isTrue();
    }

    @Test
    @DisplayName("버디 찌르기 대상은 본인을 제외하고 오늘 기록 및 콕찌르기 여부와 프로필 URL을 반환한다.")
    void getNudgeTargets() {
        ChallengeHomeService service = service();
        GroupMember currentMember = groupMember(1L, "민석", LocalDateTime.now().minusDays(2));
        GroupMember recordedBuddy = groupMember(2L, "기록친구", LocalDateTime.now().minusDays(1));
        GroupMember notRecordedBuddy = groupMember(3L, "미기록친구", LocalDateTime.now().minusDays(1));
        givenValidGroupMembership(1L, currentMember);
        givenJoinedMembers(List.of(currentMember, recordedBuddy, notRecordedBuddy));
        given(activityRecordRepository.findActiveGroupRecordsBetween(any(), any(), any(), any()))
            .willReturn(List.of(mealRecord(2L, LocalDate.now().atTime(9, 0))));

        NudgeTargetListResult result = service.getNudgeTargets(1L, 10L);

        assertThat(result.members())
            .extracting("memberId", "nickname", "profileImageUrl", "recordedToday", "nudgedToday", "buttonStatus")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    2L,
                    "기록친구",
                    "https://storage.example.com/profiles/member-2.jpg",
                    true,
                    false,
                    NudgeButtonStatus.RECORDED
                ),
                org.assertj.core.groups.Tuple.tuple(
                    3L,
                    "미기록친구",
                    "https://storage.example.com/profiles/member-3.jpg",
                    false,
                    false,
                    NudgeButtonStatus.AVAILABLE
                )
            );
    }

    @Test
    @DisplayName("버디 찌르기 대상은 같은 그룹에서 오늘 이미 콕찌른 대상을 반환한다.")
    void getNudgeTargetsWithNudgedBuddy() {
        ChallengeHomeService service = service();
        GroupMember currentMember = groupMember(1L, "민석", LocalDateTime.now().minusDays(2));
        GroupMember buddy = groupMember(2L, "친구", LocalDateTime.now().minusDays(1));
        givenValidGroupMembership(1L, currentMember);
        givenJoinedMembers(List.of(currentMember, buddy));
        given(activityRecordRepository.findActiveGroupRecordsBetween(any(), any(), any(), any())).willReturn(List.of());
        given(notificationRepository
            .findByNotificationTypeAndReferenceIdAndReceiverMemberIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
                any(), any(), any(), any(), any()
            ))
            .willReturn(List.of(buddyNudge(2L, BuddyNudgeDedupeKey.create(10L, 1L, 2L, LocalDate.now().toString()))));

        NudgeTargetListResult result = service.getNudgeTargets(1L, 10L);

        assertThat(result.members()).singleElement()
            .extracting("memberId", "recordedToday", "nudgedToday", "buttonStatus")
            .containsExactly(2L, false, true, NudgeButtonStatus.NUDGED);
    }

    @Test
    @DisplayName("오늘 기록을 완료한 버디는 이미 콕찌른 이력이 있어도 기록 완료 상태를 우선 반환한다.")
    void getNudgeTargetsPrioritizesRecordedStatus() {
        ChallengeHomeService service = service();
        GroupMember currentMember = groupMember(1L, "민석", LocalDateTime.now().minusDays(2));
        GroupMember buddy = groupMember(2L, "친구", LocalDateTime.now().minusDays(1));
        givenValidGroupMembership(1L, currentMember);
        givenJoinedMembers(List.of(currentMember, buddy));
        given(activityRecordRepository.findActiveGroupRecordsBetween(any(), any(), any(), any()))
            .willReturn(List.of(mealRecord(2L, LocalDate.now().atTime(9, 0))));
        given(notificationRepository
            .findByNotificationTypeAndReferenceIdAndReceiverMemberIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
                any(), any(), any(), any(), any()
            ))
            .willReturn(List.of(buddyNudge(2L, BuddyNudgeDedupeKey.create(10L, 1L, 2L, LocalDate.now().toString()))));

        NudgeTargetListResult result = service.getNudgeTargets(1L, 10L);

        assertThat(result.members()).singleElement()
            .extracting("recordedToday", "nudgedToday", "buttonStatus")
            .containsExactly(true, true, NudgeButtonStatus.RECORDED);
    }

    @Test
    @DisplayName("버디 찌르기는 대상 회원에게 알림 요청을 발행한다.")
    void nudgeMember() {
        ChallengeHomeService service = service();
        GroupMember sender = groupMember(1L, "민석", LocalDateTime.now().minusDays(2));
        GroupMember receiver = groupMember(2L, "친구", LocalDateTime.now().minusDays(1));
        givenValidGroupMembership(1L, sender);
        given(memberRepository.findById(2L)).willReturn(Optional.of(member(2L)));
        given(groupMemberRepository.findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            2L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(Optional.of(receiver));

        service.nudgeMember(1L, 10L, 2L);

        then(notificationRequestService).should()
            .requestBuddyNudge(10L, 1L, "민석", 2L, LocalDate.now().toString());
    }

    @Test
    @DisplayName("같은 버디에게 같은 날 다시 콕찌르기를 요청할 수 없다.")
    void throwWhenNudgingSameBuddyAgainToday() {
        ChallengeHomeService service = service();
        GroupMember sender = groupMember(1L, "민석", LocalDateTime.now().minusDays(2));
        GroupMember receiver = groupMember(2L, "친구", LocalDateTime.now().minusDays(1));
        givenValidGroupMembership(1L, sender);
        given(memberRepository.findById(2L)).willReturn(Optional.of(member(2L)));
        given(groupMemberRepository.findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            2L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(Optional.of(receiver));
        given(notificationRepository.existsByDedupeKeyAndDeletedAtIsNull(
            BuddyNudgeDedupeKey.create(10L, 1L, 2L, LocalDate.now().toString())
        )).willReturn(true);

        assertThatThrownBy(() -> service.nudgeMember(1L, 10L, 2L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.CHALLENGE_NUDGE_ALREADY_SENT));
        then(notificationRequestService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("본인에게 버디 찌르기를 요청할 수 없다.")
    void throwSelfNudge() {
        ChallengeHomeService service = service();

        assertThatThrownBy(() -> service.nudgeMember(1L, 10L, 1L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.CHALLENGE_VALIDATION_FAILED));
    }

    private ChallengeHomeService service() {
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.setBaseUrl("https://storage.example.com");
        return new ChallengeHomeService(
            memberRepository,
            modyGroupRepository,
            groupMemberRepository,
            activityRecordRepository,
            groupChallengeRepository,
            notificationRequestService,
            notificationRepository,
            new ImageUrlResolver(uploadProperties)
        );
    }

    private void givenValidGroupMembership(Long memberId, GroupMember groupMember) {
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member(memberId)));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디")));
        given(groupMemberRepository.findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(Optional.of(groupMember));
    }

    private void givenJoinedMembers(List<GroupMember> groupMembers) {
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(groupMembers);
    }

    private void givenGroupMemberHistory(List<GroupMember> groupMembers) {
        given(groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(10L)).willReturn(groupMembers);
    }

    private Member member(Long memberId) {
        return new Member(memberId, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }

    private GroupMember groupMember(Long memberId, String nickname, LocalDateTime joinedAt) {
        return new GroupMember(
            memberId + 100,
            memberId,
            10L,
            nickname,
            "profiles/member-" + memberId + ".jpg",
            joinedAt
        );
    }

    private ActivityRecord mealRecord(Long memberId, LocalDateTime uploadedAt) {
        return ActivityRecord.meal(
            memberId + uploadedAt.getDayOfMonth(),
            memberId,
            10L,
            LocalTime.of(8, 0),
            "샐러드",
            "records/meal.jpg",
            uploadedAt
        );
    }

    private ActivityRecord exerciseRecord(Long memberId, LocalDateTime uploadedAt, int minutes) {
        return ActivityRecord.exercise(
            memberId + uploadedAt.getDayOfMonth() + 100,
            memberId,
            10L,
            minutes,
            "러닝",
            "records/exercise.jpg",
            uploadedAt
        );
    }

    private Notification buddyNudge(Long receiverMemberId, String dedupeKey) {
        return new Notification(
            100L,
            receiverMemberId,
            NotificationType.BUDDY_NUDGE,
            "민석님이 콕 찔렀어요!",
            "민석님의 응원을 받고 얼른 기록해주세요!",
            null,
            "GROUP",
            10L,
            LocalDateTime.now(),
            3,
            dedupeKey
        );
    }
}
