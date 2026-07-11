package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.challenge.application.StepChallengeService.StepChallengeChangeCommand;
import cmc.mody.challenge.application.StepChallengeService.StepChallengeChangeResult;
import cmc.mody.challenge.application.StepChallengeService.StepChallengeStatusResult;
import cmc.mody.challenge.application.StepChallengeService.StepRankingListResult;
import cmc.mody.challenge.application.StepChallengeService.WalkedRegionListResult;
import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.domain.StepChallengeDetail;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.StepChallengeDetailRepository;
import cmc.mody.challenge.infrastructure.repository.StepRecordRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class StepChallengeServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private StepChallengeDetailRepository stepChallengeDetailRepository;

    @Mock
    private GroupChallengeRepository groupChallengeRepository;

    @Mock
    private StepRecordRepository stepRecordRepository;

    @Captor
    private ArgumentCaptor<GroupChallenge> groupChallengeCaptor;

    @Test
    @DisplayName("현재 걸음수 챌린지는 그룹 챌린지와 누적 걸음수를 반환한다.")
    void getCurrentStepChallenge() {
        StepChallengeService service = service();
        GroupChallenge groupChallenge = new GroupChallenge(100L, 10L, 1L, LocalDate.now(), LocalDate.of(9999, 12, 31));
        givenValidGroupMembership();
        givenStepChallenges(List.of(challenge(1L, "서울-인천")));
        given(groupChallengeRepository.findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
            10L,
            List.of(1L),
            GroupChallengeStatus.IN_PROGRESS
        )).willReturn(Optional.of(groupChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(1L, ChallengeType.STEP))
            .willReturn(Optional.of(challenge(1L, "서울-인천")));
        given(stepChallengeDetailRepository.findByChallengeIdAndDeletedAtIsNull(1L))
            .willReturn(Optional.of(stepDetail(1L, "인천", 150_000)));
        given(stepRecordRepository.sumStepCountByGroupChallengeId(100L)).willReturn(34_000L);

        StepChallengeStatusResult result = service.getCurrentStepChallenge(1L, 10L);

        assertThat(result.groupChallengeId()).isEqualTo(100L);
        assertThat(result.title()).isEqualTo("서울-인천");
        assertThat(result.targetStepCount()).isEqualTo(150_000);
        assertThat(result.currentStepCount()).isEqualTo(34_000);
    }

    @Test
    @DisplayName("완료한 걸음수 챌린지 목적지를 걸어간 지역으로 반환한다.")
    void getWalkedRegions() {
        StepChallengeService service = service();
        GroupChallenge incheon = new GroupChallenge(100L, 10L, 1L, LocalDate.now(), LocalDate.of(9999, 12, 31));
        GroupChallenge cheonan = new GroupChallenge(101L, 10L, 2L, LocalDate.now(), LocalDate.of(9999, 12, 31));
        incheon.complete(LocalDateTime.now());
        cheonan.complete(LocalDateTime.now());
        givenValidGroupMembership();
        givenStepChallenges(List.of(challenge(1L, "서울-인천"), challenge(2L, "서울-천안")));
        given(stepChallengeDetailRepository.findByChallengeIdInAndDeletedAtIsNull(List.of(1L, 2L)))
            .willReturn(List.of(
                stepDetail(1L, "인천", 150_000),
                stepDetail(2L, "천안", 200_000)
            ));
        given(groupChallengeRepository
            .findByGroupIdAndChallengeIdInAndGroupChallengeStatusInAndDeletedAtIsNullOrderByEndedAtAscIdAsc(
                10L,
                List.of(1L, 2L),
                List.of(GroupChallengeStatus.COMPLETED)
            ))
            .willReturn(List.of(incheon, cheonan));

        WalkedRegionListResult result = service.getWalkedRegions(1L, 10L);

        assertThat(result.regions())
            .extracting("regionName", "regionImageUrl")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("인천", "regions/인천.png"),
                org.assertj.core.groups.Tuple.tuple("천안", "regions/천안.png")
            );
    }

    @Test
    @DisplayName("변경 가능한 걸음수 챌린지 목록은 노출 순서와 현재 선택 여부를 반환한다.")
    void getStepChallengeOptions() {
        StepChallengeService service = service();
        GroupChallenge current = new GroupChallenge(100L, 10L, 2L, LocalDate.now(), LocalDate.of(9999, 12, 31));
        givenValidGroupMembership();
        givenStepChallenges(List.of(challenge(1L, "서울-인천"), challenge(2L, "서울-천안")));
        given(groupChallengeRepository.findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
            10L,
            List.of(1L, 2L),
            GroupChallengeStatus.IN_PROGRESS
        )).willReturn(Optional.of(current));
        given(stepChallengeDetailRepository.findByChallengeIdInAndDeletedAtIsNull(List.of(1L, 2L)))
            .willReturn(List.of(
                stepDetail(2L, "천안", 200_000),
                stepDetail(1L, "인천", 150_000)
            ));

        StepChallengeService.StepChallengeOptionListResult result = service.getStepChallengeOptions(1L, 10L);

        assertThat(result.options())
            .extracting("challengeId", "title", "destination", "targetStepCount", "selected")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1L, "서울-인천", "인천", 150_000, false),
                org.assertj.core.groups.Tuple.tuple(2L, "서울-천안", "천안", 200_000, true)
            );
    }

    @Test
    @DisplayName("기여도 순위는 걸음수 내림차순, 가입일 오름차순으로 반환한다.")
    void getStepRankings() {
        StepChallengeService service = service();
        GroupChallenge groupChallenge = new GroupChallenge(100L, 10L, 1L, LocalDate.now(), LocalDate.of(9999, 12, 31));
        givenValidGroupMembership();
        givenStepChallenges(List.of(challenge(1L, "서울-인천")));
        given(groupChallengeRepository.findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
            10L,
            List.of(1L),
            GroupChallengeStatus.IN_PROGRESS
        )).willReturn(Optional.of(groupChallenge));
        given(stepRecordRepository.sumStepCountByMember(100L))
            .willReturn(List.of(new Object[]{3L, 20_000L}, new Object[]{1L, 10_000L}));
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            groupMember(1L, "민석", LocalDateTime.of(2026, 1, 1, 10, 0)),
            groupMember(2L, "예은", LocalDateTime.of(2026, 1, 2, 10, 0)),
            groupMember(3L, "도윤", LocalDateTime.of(2026, 1, 3, 10, 0))
        ));

        StepRankingListResult result = service.getStepRankings(1L, 10L);

        assertThat(result.rankings())
            .extracting("rank", "memberId", "nickname", "stepCount")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1, 3L, "도윤", 20_000),
                org.assertj.core.groups.Tuple.tuple(2, 1L, "민석", 10_000),
                org.assertj.core.groups.Tuple.tuple(3, 2L, "예은", 0)
            );
    }

    @Test
    @DisplayName("챌린지를 변경하면 기존 진행 챌린지를 초기화하고 새 그룹 챌린지를 만든다.")
    void changeStepChallenge() {
        StepChallengeService service = service();
        GroupChallenge current = new GroupChallenge(100L, 10L, 1L, LocalDate.now(), LocalDate.of(9999, 12, 31));
        givenValidGroupMembership();
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(2L, ChallengeType.STEP))
            .willReturn(Optional.of(challenge(2L, "서울-천안")));
        given(stepChallengeDetailRepository.findByChallengeIdAndDeletedAtIsNull(2L))
            .willReturn(Optional.of(stepDetail(2L, "천안", 200_000)));
        givenStepChallenges(List.of(challenge(1L, "서울-인천"), challenge(2L, "서울-천안")));
        given(groupChallengeRepository.findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
            10L,
            List.of(1L, 2L),
            GroupChallengeStatus.IN_PROGRESS
        )).willReturn(Optional.of(current));
        given(idGenerator.nextId()).willReturn(200L);
        given(groupChallengeRepository.save(any(GroupChallenge.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        StepChallengeChangeResult result = service.changeStepChallenge(1L, 10L, new StepChallengeChangeCommand(2L));

        assertThat(current.getGroupChallengeStatus()).isEqualTo(GroupChallengeStatus.RESET);
        assertThat(current.getEndedAt()).isNotNull();
        then(groupChallengeRepository).should().save(groupChallengeCaptor.capture());
        assertThat(groupChallengeCaptor.getValue().getId()).isEqualTo(200L);
        assertThat(groupChallengeCaptor.getValue().getGroupId()).isEqualTo(10L);
        assertThat(groupChallengeCaptor.getValue().getChallengeId()).isEqualTo(2L);
        assertThat(result).isEqualTo(new StepChallengeChangeResult(200L, 2L, "서울-천안", 200_000, 0));
    }

    @Test
    @DisplayName("같은 챌린지로 변경 요청하면 진행률을 초기화하지 않고 현재 상태를 반환한다.")
    void changeSameStepChallenge() {
        StepChallengeService service = service();
        GroupChallenge current = new GroupChallenge(100L, 10L, 2L, LocalDate.now(), LocalDate.of(9999, 12, 31));
        givenValidGroupMembership();
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(2L, ChallengeType.STEP))
            .willReturn(Optional.of(challenge(2L, "서울-천안")));
        given(stepChallengeDetailRepository.findByChallengeIdAndDeletedAtIsNull(2L))
            .willReturn(Optional.of(stepDetail(2L, "천안", 200_000)));
        givenStepChallenges(List.of(challenge(1L, "서울-인천"), challenge(2L, "서울-천안")));
        given(groupChallengeRepository.findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
            10L,
            List.of(1L, 2L),
            GroupChallengeStatus.IN_PROGRESS
        )).willReturn(Optional.of(current));
        given(stepRecordRepository.sumStepCountByGroupChallengeId(100L)).willReturn(12_345L);

        StepChallengeChangeResult result = service.changeStepChallenge(1L, 10L, new StepChallengeChangeCommand(2L));

        assertThat(current.getGroupChallengeStatus()).isEqualTo(GroupChallengeStatus.IN_PROGRESS);
        assertThat(current.getEndedAt()).isNull();
        then(groupChallengeRepository).should(org.mockito.Mockito.never()).save(any(GroupChallenge.class));
        assertThat(result).isEqualTo(new StepChallengeChangeResult(100L, 2L, "서울-천안", 200_000, 12_345));
    }

    @Test
    @DisplayName("그룹에 참여하지 않은 회원은 걸음수 챌린지를 조회할 수 없다.")
    void throwGroupMemberNotFound() {
        StepChallengeService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(false);

        assertThatThrownBy(() -> service.getCurrentStepChallenge(1L, 10L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
    }

    private StepChallengeService service() {
        return new StepChallengeService(
            idGenerator,
            memberRepository,
            modyGroupRepository,
            groupMemberRepository,
            challengeRepository,
            stepChallengeDetailRepository,
            groupChallengeRepository,
            stepRecordRepository
        );
    }

    private void givenValidGroupMembership() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
    }

    private void givenStepChallenges(List<Challenge> challenges) {
        given(challengeRepository.findByChallengeTypeAndDeletedAtIsNull(ChallengeType.STEP)).willReturn(challenges);
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }

    private Challenge challenge(Long id, String title) {
        return new Challenge(id, ChallengeType.STEP, title, title + " 챌린지");
    }

    private StepChallengeDetail stepDetail(Long challengeId, String destination, int targetStepCount) {
        return new StepChallengeDetail(
            challengeId + 1000,
            challengeId,
            "서울",
            destination,
            BigDecimal.valueOf(60.0),
            targetStepCount,
            challengeId.intValue()
        );
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
}
