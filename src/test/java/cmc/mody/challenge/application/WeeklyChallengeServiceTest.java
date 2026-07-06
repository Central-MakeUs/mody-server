package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeProofCreateCommand;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeProofCreateResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeProofListResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeListResult;
import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeProof;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.infrastructure.repository.ChallengeProofRepository;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.common.upload.UploadProperties;
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
class WeeklyChallengeServiceTest {
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
    private GroupChallengeRepository groupChallengeRepository;

    @Mock
    private ChallengeProofRepository challengeProofRepository;

    @Captor
    private ArgumentCaptor<ChallengeProof> proofCaptor;

    @Test
    @DisplayName("이번 주 주간 챌린지는 진행 중인 PHOTO 그룹 챌린지와 인증 참여 현황을 반환한다.")
    void getWeeklyChallenges() {
        WeeklyChallengeService service = service();
        GroupChallenge groupChallenge = groupChallenge(100L, 10L, 1L);
        ChallengeProof proof = proof(1000L, 100L, 2L, "weekly-challenges/2/proof.jpg");
        givenValidGroupMembership();
        givenWeeklyChallenges(List.of(challenge(1L, "물 2L 마시기")));
        given(groupChallengeRepository
            .findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndStartsOnLessThanEqualAndEndsOnGreaterThanEqualAndDeletedAtIsNullOrderByEndsOnAscIdAsc(
                any(),
                any(),
                any(),
                any(),
                any()
            ))
            .willReturn(List.of(groupChallenge));
        given(challengeProofRepository.findByGroupChallengeIdInAndDeletedAtIsNullOrderByUploadedAtAscIdAsc(List.of(100L)))
            .willReturn(List.of(proof));
        givenJoinedMembers();

        WeeklyChallengeListResult result = service.getWeeklyChallenges(1L, 10L);

        assertThat(result.challenges())
            .extracting("groupChallengeId", "title", "participantCount", "randomParticipantNickname")
            .containsExactly(org.assertj.core.groups.Tuple.tuple(100L, "물 2L 마시기", 1, "친구"));
    }

    @Test
    @DisplayName("주간 챌린지 상세는 PHOTO 타입 챌린지 정보만 반환한다.")
    void getWeeklyChallengeDetail() {
        WeeklyChallengeService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(1L, ChallengeType.PHOTO))
            .willReturn(Optional.of(challenge(1L, "물 2L 마시기")));

        WeeklyChallengeService.WeeklyChallengeDetailResult result = service.getWeeklyChallengeDetail(1L, 1L);

        assertThat(result.challengeId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("물 2L 마시기");
        assertThat(result.description()).isEqualTo("물 2L 마시기 설명");
    }

    @Test
    @DisplayName("그룹원 인증 이미지는 이미지 URL과 그룹 내 회원 표시 정보를 함께 반환한다.")
    void getWeeklyChallengeProofs() {
        WeeklyChallengeService service = service();
        GroupChallenge groupChallenge = groupChallenge(100L, 10L, 1L);
        givenValidGroupMembership();
        given(groupChallengeRepository.findByIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(groupChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(1L, ChallengeType.PHOTO))
            .willReturn(Optional.of(challenge(1L, "물 2L 마시기")));
        givenJoinedMembers();
        given(challengeProofRepository.findByGroupChallengeIdAndDeletedAtIsNullOrderByUploadedAtAscIdAsc(100L))
            .willReturn(List.of(proof(1000L, 100L, 2L, "weekly-challenges/2/proof.jpg")));

        WeeklyChallengeProofListResult result = service.getWeeklyChallengeProofs(1L, 10L, 100L);

        assertThat(result.proofs())
            .extracting("proofId", "imageUrl", "memberId", "nickname", "profileImageUrl")
            .containsExactly(org.assertj.core.groups.Tuple.tuple(
                1000L,
                "https://storage.example.com/weekly-challenges/2/proof.jpg",
                2L,
                "친구",
                "https://storage.example.com/profiles/member-2.jpg"
            ));
    }

    @Test
    @DisplayName("주간 챌린지 인증 이미지를 한 번 저장한다.")
    void createWeeklyChallengeProof() {
        WeeklyChallengeService service = service();
        GroupChallenge groupChallenge = groupChallenge(100L, 10L, 1L);
        givenValidGroupMembership();
        given(groupChallengeRepository.findByIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(groupChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(1L, ChallengeType.PHOTO))
            .willReturn(Optional.of(challenge(1L, "물 2L 마시기")));
        given(challengeProofRepository.existsByGroupChallengeIdAndMemberIdAndDeletedAtIsNull(100L, 1L))
            .willReturn(false);
        given(idGenerator.nextId()).willReturn(200L);
        given(challengeProofRepository.save(any(ChallengeProof.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        WeeklyChallengeProofCreateResult result = service.createWeeklyChallengeProof(
            1L,
            10L,
            100L,
            new WeeklyChallengeProofCreateCommand("weekly-challenges/1/proof.jpg")
        );

        then(challengeProofRepository).should().save(proofCaptor.capture());
        assertThat(proofCaptor.getValue().getGroupChallengeId()).isEqualTo(100L);
        assertThat(proofCaptor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(result).isEqualTo(new WeeklyChallengeProofCreateResult(
            200L,
            100L,
            "https://storage.example.com/weekly-challenges/1/proof.jpg"
        ));
    }

    @Test
    @DisplayName("이미 인증한 주간 챌린지는 중복 인증할 수 없다.")
    void throwProofAlreadyExists() {
        WeeklyChallengeService service = service();
        GroupChallenge groupChallenge = groupChallenge(100L, 10L, 1L);
        givenValidGroupMembership();
        given(groupChallengeRepository.findByIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(groupChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(1L, ChallengeType.PHOTO))
            .willReturn(Optional.of(challenge(1L, "물 2L 마시기")));
        given(challengeProofRepository.existsByGroupChallengeIdAndMemberIdAndDeletedAtIsNull(100L, 1L))
            .willReturn(true);

        assertThatThrownBy(() -> service.createWeeklyChallengeProof(
            1L,
            10L,
            100L,
            new WeeklyChallengeProofCreateCommand("weekly-challenges/1/proof.jpg")
        ))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.CHALLENGE_PROOF_ALREADY_EXISTS));
    }

    private WeeklyChallengeService service() {
        UploadProperties uploadProperties = new UploadProperties();
        uploadProperties.setBaseUrl("https://storage.example.com");
        return new WeeklyChallengeService(
            idGenerator,
            memberRepository,
            modyGroupRepository,
            groupMemberRepository,
            challengeRepository,
            groupChallengeRepository,
            challengeProofRepository,
            uploadProperties
        );
    }

    private void givenValidGroupMembership() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABC123", "모디")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
    }

    private void givenWeeklyChallenges(List<Challenge> challenges) {
        given(challengeRepository.findByChallengeTypeAndDeletedAtIsNull(ChallengeType.PHOTO)).willReturn(challenges);
    }

    private void givenJoinedMembers() {
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            groupMember(1L, "민석"),
            groupMember(2L, "친구")
        ));
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }

    private Challenge challenge(Long id, String title) {
        return new Challenge(id, ChallengeType.PHOTO, title, title + " 설명");
    }

    private GroupChallenge groupChallenge(Long id, Long groupId, Long challengeId) {
        return new GroupChallenge(id, groupId, challengeId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));
    }

    private ChallengeProof proof(Long id, Long groupChallengeId, Long memberId, String imageKey) {
        return new ChallengeProof(id, groupChallengeId, memberId, imageKey, LocalDateTime.of(2026, 7, 5, 10, 0));
    }

    private GroupMember groupMember(Long memberId, String nickname) {
        return new GroupMember(
            memberId + 100,
            memberId,
            10L,
            nickname,
            "profiles/member-" + memberId + ".jpg",
            LocalDateTime.of(2026, 7, 1, 10, 0)
        );
    }
}
