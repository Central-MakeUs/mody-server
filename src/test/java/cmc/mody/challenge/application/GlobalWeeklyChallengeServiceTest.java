package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeCommand;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeCreateResult;
import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GlobalWeeklyChallenge;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GlobalWeeklyChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalWeeklyChallengeServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private GlobalWeeklyChallengeRepository globalWeeklyChallengeRepository;

    @Mock
    private GroupChallengeRepository groupChallengeRepository;

    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Captor
    private ArgumentCaptor<GroupChallenge> groupChallengeCaptor;

    @Test
    @DisplayName("전역 주간 챌린지를 한 번 등록하고 활성 그룹별 진행 인스턴스를 원본에 연결한다.")
    void createGlobalWeeklyChallenge() {
        GlobalWeeklyChallengeService service = service();
        LocalDate startsOn = LocalDate.of(2026, 8, 10);
        LocalDate endsOn = LocalDate.of(2026, 8, 16);
        given(idGenerator.nextId()).willReturn(100L, 200L, 300L, 400L);
        given(challengeRepository.save(any(Challenge.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(globalWeeklyChallengeRepository.save(any(GlobalWeeklyChallenge.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(modyGroupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(List.of(
            new ModyGroup(10L, "GROUP001", "첫 번째 그룹"),
            new ModyGroup(20L, "GROUP002", "두 번째 그룹")
        ));

        GlobalWeeklyChallengeCreateResult result = service.create(new GlobalWeeklyChallengeCommand(
            "엘리베이터 안 타고 올라가기",
            "계단으로 이동한 사진을 인증해주세요.",
            startsOn,
            endsOn
        ));

        assertThat(result).isEqualTo(new GlobalWeeklyChallengeCreateResult(
            200L,
            100L,
            "엘리베이터 안 타고 올라가기",
            "계단으로 이동한 사진을 인증해주세요.",
            startsOn,
            endsOn,
            2
        ));
        then(groupChallengeRepository).should().saveAll(argThat(instances -> {
            List<GroupChallenge> linkedInstances = StreamSupport.stream(instances.spliterator(), false).toList();
            return linkedInstances.size() == 2
                && linkedInstances.stream().allMatch(instance -> instance.getChallengeId().equals(100L))
                && linkedInstances.stream().allMatch(instance -> instance.getGlobalWeeklyChallengeId().equals(200L))
                && linkedInstances.stream().allMatch(instance -> instance.getStartsOn().equals(startsOn))
                && linkedInstances.stream().allMatch(instance -> instance.getEndsOn().equals(endsOn));
        }));
    }

    @Test
    @DisplayName("새 그룹은 진행 중인 전역 주간 챌린지 진행 인스턴스를 자동으로 연결한다.")
    void initializeForNewGroup() {
        GlobalWeeklyChallengeService service = service();
        LocalDate today = LocalDate.now();
        GlobalWeeklyChallenge globalWeeklyChallenge = new GlobalWeeklyChallenge(
            100L,
            200L,
            today.minusDays(1),
            today.plusDays(5)
        );
        given(globalWeeklyChallengeRepository.findByStartsOnLessThanEqualAndEndsOnGreaterThanEqualAndDeletedAtIsNull(
            today,
            today
        )).willReturn(List.of(globalWeeklyChallenge));
        given(groupChallengeRepository.existsByGroupIdAndGlobalWeeklyChallengeIdAndDeletedAtIsNull(10L, 100L))
            .willReturn(false);
        given(idGenerator.nextId()).willReturn(300L);

        service.initializeForNewGroup(10L);

        then(groupChallengeRepository).should().save(groupChallengeCaptor.capture());
        GroupChallenge groupChallenge = groupChallengeCaptor.getValue();
        assertThat(groupChallenge.getGroupId()).isEqualTo(10L);
        assertThat(groupChallenge.getChallengeId()).isEqualTo(200L);
        assertThat(groupChallenge.getGlobalWeeklyChallengeId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("전역 주간 챌린지를 수정하면 연결된 그룹 진행 인스턴스의 기간도 함께 수정한다.")
    void updateGlobalWeeklyChallenge() {
        GlobalWeeklyChallengeService service = service();
        GlobalWeeklyChallenge globalWeeklyChallenge = new GlobalWeeklyChallenge(
            100L,
            200L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        Challenge challenge = new Challenge(200L, ChallengeType.PHOTO, "기존 제목", "기존 설명");
        GroupChallenge firstGroupChallenge = new GroupChallenge(
            300L,
            10L,
            200L,
            100L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        GroupChallenge secondGroupChallenge = new GroupChallenge(
            400L,
            20L,
            200L,
            100L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        LocalDate startsOn = LocalDate.of(2026, 8, 10);
        LocalDate endsOn = LocalDate.of(2026, 8, 16);
        given(globalWeeklyChallengeRepository.findByIdAndDeletedAtIsNull(100L))
            .willReturn(Optional.of(globalWeeklyChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(200L, ChallengeType.PHOTO))
            .willReturn(Optional.of(challenge));
        given(groupChallengeRepository.findByGlobalWeeklyChallengeIdAndDeletedAtIsNull(100L))
            .willReturn(List.of(firstGroupChallenge, secondGroupChallenge));
        given(groupChallengeRepository.countByGlobalWeeklyChallengeIdAndDeletedAtIsNull(100L)).willReturn(2L);

        GlobalWeeklyChallengeService.GlobalWeeklyChallengeResult result = service.update(
            100L,
            new GlobalWeeklyChallengeCommand("새 제목", "새 설명", startsOn, endsOn)
        );

        assertThat(challenge.getTitle()).isEqualTo("새 제목");
        assertThat(challenge.getDescription()).isEqualTo("새 설명");
        assertThat(firstGroupChallenge.getStartsOn()).isEqualTo(startsOn);
        assertThat(secondGroupChallenge.getEndsOn()).isEqualTo(endsOn);
        assertThat(result.linkedGroupCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("전역 주간 챌린지를 삭제하면 원본과 사진 챌린지 메타데이터를 함께 비활성화한다.")
    void deleteGlobalWeeklyChallenge() {
        GlobalWeeklyChallengeService service = service();
        GlobalWeeklyChallenge globalWeeklyChallenge = new GlobalWeeklyChallenge(
            100L,
            200L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        Challenge challenge = new Challenge(200L, ChallengeType.PHOTO, "제목", "설명");
        given(globalWeeklyChallengeRepository.findByIdAndDeletedAtIsNull(100L))
            .willReturn(Optional.of(globalWeeklyChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(200L, ChallengeType.PHOTO))
            .willReturn(Optional.of(challenge));

        service.delete(100L);

        assertThat(globalWeeklyChallenge.isActive()).isFalse();
        assertThat(challenge.isActive()).isFalse();
        then(groupChallengeRepository).shouldHaveNoInteractions();
    }

    private GlobalWeeklyChallengeService service() {
        return new GlobalWeeklyChallengeService(
            idGenerator,
            challengeRepository,
            globalWeeklyChallengeRepository,
            groupChallengeRepository,
            modyGroupRepository
        );
    }
}
