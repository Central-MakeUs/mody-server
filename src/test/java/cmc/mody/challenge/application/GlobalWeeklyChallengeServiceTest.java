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
    @DisplayName("전역 주간 챌린지를 등록해도 그룹별 Challenge와 진행 인스턴스는 별도로 생성한다.")
    void createGlobalWeeklyChallenge() {
        GlobalWeeklyChallengeService service = service();
        LocalDate startsOn = LocalDate.of(2026, 8, 10);
        LocalDate endsOn = LocalDate.of(2026, 8, 16);
        given(idGenerator.nextId()).willReturn(100L, 200L, 300L, 400L, 500L, 600L);
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
                && linkedInstances.stream().map(GroupChallenge::getChallengeId).distinct().count() == 2
                && linkedInstances.stream().allMatch(instance -> instance.getGlobalWeeklyChallengeId().equals(200L));
        }));
        then(groupChallengeRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("같은 멱등성 키로 재요청하면 기존 전역 주간 챌린지를 반환한다.")
    void createGlobalWeeklyChallengeIsIdempotent() {
        GlobalWeeklyChallengeService service = service();
        GlobalWeeklyChallenge existing = new GlobalWeeklyChallenge(
            100L,
            200L,
            "weekly-2026-08-10",
            LocalDate.of(2026, 8, 10),
            LocalDate.of(2026, 8, 16)
        );
        Challenge sourceChallenge = new Challenge(200L, ChallengeType.PHOTO, "기존 제목", "기존 설명");
        given(globalWeeklyChallengeRepository.findByIdempotencyKeyAndDeletedAtIsNull("weekly-2026-08-10"))
            .willReturn(Optional.of(existing));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(200L, ChallengeType.PHOTO))
            .willReturn(Optional.of(sourceChallenge));
        given(groupChallengeRepository.countByGlobalWeeklyChallengeIdAndDeletedAtIsNull(100L)).willReturn(3L);

        GlobalWeeklyChallengeCreateResult result = service.create(
            "weekly-2026-08-10",
            new GlobalWeeklyChallengeCommand(
                "새 제목",
                "새 설명",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 16)
            )
        );

        assertThat(result.globalWeeklyChallengeId()).isEqualTo(100L);
        assertThat(result.challengeId()).isEqualTo(200L);
        assertThat(result.linkedGroupCount()).isEqualTo(3);
        then(challengeRepository).shouldHaveNoMoreInteractions();
        then(groupChallengeRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("전역 주간 챌린지 그룹 동기화는 누락된 활성 그룹만 추가한다.")
    void syncGroups() {
        GlobalWeeklyChallengeService service = service();
        GlobalWeeklyChallenge globalWeeklyChallenge = new GlobalWeeklyChallenge(
            100L,
            200L,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(5)
        );
        Challenge sourceChallenge = new Challenge(200L, ChallengeType.PHOTO, "제목", "설명");
        given(globalWeeklyChallengeRepository.findByIdAndDeletedAtIsNull(100L))
            .willReturn(Optional.of(globalWeeklyChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(200L, ChallengeType.PHOTO))
            .willReturn(Optional.of(sourceChallenge));
        given(modyGroupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(List.of(
            new ModyGroup(10L, "GROUP001", "연결된 그룹"),
            new ModyGroup(20L, "GROUP002", "누락된 그룹")
        ));
        given(groupChallengeRepository.existsByGroupIdAndGlobalWeeklyChallengeIdAndDeletedAtIsNull(10L, 100L))
            .willReturn(true);
        given(groupChallengeRepository.existsByGroupIdAndGlobalWeeklyChallengeIdAndDeletedAtIsNull(20L, 100L))
            .willReturn(false);
        given(challengeRepository.save(any(Challenge.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(idGenerator.nextId()).willReturn(300L, 400L);

        GlobalWeeklyChallengeService.GlobalWeeklyChallengeSyncResult result = service.syncGroups(100L);

        assertThat(result).isEqualTo(new GlobalWeeklyChallengeService.GlobalWeeklyChallengeSyncResult(100L, 1));
        then(groupChallengeRepository).should().saveAll(argThat(instances ->
            StreamSupport.stream(instances.spliterator(), false).count() == 1
        ));
    }

    @Test
    @DisplayName("새 그룹은 진행 중인 전역 주간 챌린지를 기존 그룹별 구조로 자동 연결한다.")
    void initializeForNewGroup() {
        GlobalWeeklyChallengeService service = service();
        LocalDate today = LocalDate.now();
        GlobalWeeklyChallenge globalWeeklyChallenge = new GlobalWeeklyChallenge(
            100L,
            200L,
            today.minusDays(1),
            today.plusDays(5)
        );
        Challenge sourceChallenge = new Challenge(200L, ChallengeType.PHOTO, "제목", "설명");
        given(globalWeeklyChallengeRepository.findByStartsOnLessThanEqualAndEndsOnGreaterThanEqualAndDeletedAtIsNull(
            today,
            today
        )).willReturn(List.of(globalWeeklyChallenge));
        given(groupChallengeRepository.existsByGroupIdAndGlobalWeeklyChallengeIdAndDeletedAtIsNull(10L, 100L))
            .willReturn(false);
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(200L, ChallengeType.PHOTO))
            .willReturn(Optional.of(sourceChallenge));
        given(challengeRepository.save(any(Challenge.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(idGenerator.nextId()).willReturn(300L, 400L);

        service.initializeForNewGroup(10L);

        then(groupChallengeRepository).should().save(groupChallengeCaptor.capture());
        GroupChallenge groupChallenge = groupChallengeCaptor.getValue();
        assertThat(groupChallenge.getGroupId()).isEqualTo(10L);
        assertThat(groupChallenge.getChallengeId()).isEqualTo(300L);
        assertThat(groupChallenge.getGlobalWeeklyChallengeId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("전역 주간 챌린지를 수정하면 그룹별 메타데이터와 기간도 함께 수정한다.")
    void updateGlobalWeeklyChallenge() {
        GlobalWeeklyChallengeService service = service();
        GlobalWeeklyChallenge globalWeeklyChallenge = new GlobalWeeklyChallenge(
            100L,
            200L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        Challenge sourceChallenge = new Challenge(200L, ChallengeType.PHOTO, "기존 제목", "기존 설명");
        Challenge firstDefinition = new Challenge(300L, ChallengeType.PHOTO, "기존 제목", "기존 설명");
        Challenge secondDefinition = new Challenge(400L, ChallengeType.PHOTO, "기존 제목", "기존 설명");
        GroupChallenge firstGroupChallenge = new GroupChallenge(
            500L,
            10L,
            300L,
            100L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        GroupChallenge secondGroupChallenge = new GroupChallenge(
            600L,
            20L,
            400L,
            100L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        LocalDate startsOn = LocalDate.of(2026, 8, 10);
        LocalDate endsOn = LocalDate.of(2026, 8, 16);
        given(globalWeeklyChallengeRepository.findByIdAndDeletedAtIsNull(100L))
            .willReturn(Optional.of(globalWeeklyChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(200L, ChallengeType.PHOTO))
            .willReturn(Optional.of(sourceChallenge));
        given(groupChallengeRepository.findByGlobalWeeklyChallengeIdAndDeletedAtIsNull(100L))
            .willReturn(List.of(firstGroupChallenge, secondGroupChallenge));
        given(challengeRepository.findAllById(List.of(300L, 400L)))
            .willReturn(List.of(firstDefinition, secondDefinition));
        given(groupChallengeRepository.countByGlobalWeeklyChallengeIdAndDeletedAtIsNull(100L)).willReturn(2L);

        GlobalWeeklyChallengeService.GlobalWeeklyChallengeResult result = service.update(
            100L,
            new GlobalWeeklyChallengeCommand("새 제목", "새 설명", startsOn, endsOn)
        );

        assertThat(sourceChallenge.getTitle()).isEqualTo("새 제목");
        assertThat(firstDefinition.getTitle()).isEqualTo("새 제목");
        assertThat(secondDefinition.getDescription()).isEqualTo("새 설명");
        assertThat(firstGroupChallenge.getStartsOn()).isEqualTo(startsOn);
        assertThat(secondGroupChallenge.getEndsOn()).isEqualTo(endsOn);
        assertThat(result.linkedGroupCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("전역 주간 챌린지를 삭제하면 원본과 그룹별 사진 챌린지 메타데이터를 비활성화한다.")
    void deleteGlobalWeeklyChallenge() {
        GlobalWeeklyChallengeService service = service();
        GlobalWeeklyChallenge globalWeeklyChallenge = new GlobalWeeklyChallenge(
            100L,
            200L,
            LocalDate.of(2026, 8, 3),
            LocalDate.of(2026, 8, 9)
        );
        Challenge sourceChallenge = new Challenge(200L, ChallengeType.PHOTO, "제목", "설명");
        Challenge firstDefinition = new Challenge(300L, ChallengeType.PHOTO, "제목", "설명");
        Challenge secondDefinition = new Challenge(400L, ChallengeType.PHOTO, "제목", "설명");
        given(globalWeeklyChallengeRepository.findByIdAndDeletedAtIsNull(100L))
            .willReturn(Optional.of(globalWeeklyChallenge));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(200L, ChallengeType.PHOTO))
            .willReturn(Optional.of(sourceChallenge));
        given(groupChallengeRepository.findByGlobalWeeklyChallengeIdAndDeletedAtIsNull(100L))
            .willReturn(List.of(
                new GroupChallenge(500L, 10L, 300L, 100L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9)),
                new GroupChallenge(600L, 20L, 400L, 100L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9))
            ));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(300L, ChallengeType.PHOTO))
            .willReturn(Optional.of(firstDefinition));
        given(challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(400L, ChallengeType.PHOTO))
            .willReturn(Optional.of(secondDefinition));

        service.delete(100L);

        assertThat(globalWeeklyChallenge.isActive()).isFalse();
        assertThat(sourceChallenge.isActive()).isFalse();
        assertThat(firstDefinition.isActive()).isFalse();
        assertThat(secondDefinition.isActive()).isFalse();
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
