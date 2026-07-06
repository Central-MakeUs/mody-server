package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.challenge.application.ChallengeHomeService.ChallengeSummaryResult;
import cmc.mody.challenge.application.ChallengeHomeService.NudgeTargetListResult;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.upload.UploadProperties;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationRequestService;
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

    @Test
    @DisplayName("챌린지 홈 요약은 가입일과 이번 달 그룹 활동 통계를 반환한다.")
    void getChallengeSummary() {
        ChallengeHomeService service = service();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime firstDay = monthStart.atTime(9, 0);
        LocalDateTime secondDay = monthStart.plusDays(1).atTime(9, 0);
        GroupMember currentMember = groupMember(1L, "민석", today.minusDays(3).atTime(10, 0));
        GroupMember buddy = groupMember(2L, "친구", today.minusDays(2).atTime(10, 0));
        givenValidGroupMembership(1L, currentMember);
        givenJoinedMembers(List.of(currentMember, buddy));
        given(activityRecordRepository.findActiveGroupRecordsBetween(any(), any(), any(), any()))
            .willReturn(List.of(
                mealRecord(1L, firstDay),
                exerciseRecord(1L, firstDay.plusHours(1), 30),
                mealRecord(2L, firstDay.plusHours(2)),
                exerciseRecord(1L, secondDay, 40)
            ));
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
        assertThat(result.monthlyExerciseMinutes()).isEqualTo(70);
        assertThat(result.monthlyCompletedChallengeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("버디 찌르기 대상은 본인을 제외하고 오늘 기록 여부와 프로필 URL을 반환한다.")
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
            .extracting("memberId", "nickname", "profileImageUrl", "recordedToday")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(
                    2L,
                    "기록친구",
                    "https://storage.example.com/profiles/member-2.jpg",
                    true
                ),
                org.assertj.core.groups.Tuple.tuple(
                    3L,
                    "미기록친구",
                    "https://storage.example.com/profiles/member-3.jpg",
                    false
                )
            );
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
            uploadProperties
        );
    }

    private void givenValidGroupMembership(Long memberId, GroupMember groupMember) {
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member(memberId)));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABC123", "모디")));
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
}
