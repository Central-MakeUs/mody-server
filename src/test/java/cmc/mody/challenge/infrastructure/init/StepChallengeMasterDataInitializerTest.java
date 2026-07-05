package cmc.mody.challenge.infrastructure.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.StepChallengeDetail;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.StepChallengeDetailRepository;
import cmc.mody.common.id.IdGenerator;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StepChallengeMasterDataInitializerTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private StepChallengeDetailRepository stepChallengeDetailRepository;

    @Captor
    private ArgumentCaptor<Challenge> challengeCaptor;

    @Captor
    private ArgumentCaptor<StepChallengeDetail> detailCaptor;

    @Test
    @DisplayName("걸음수 챌린지 마스터 데이터 6개를 생성한다.")
    void initializeStepChallengeMasterData() {
        StepChallengeMasterDataInitializer initializer = initializer(true);
        given(challengeRepository.findByChallengeTypeAndTitleAndDeletedAtIsNull(any(), any()))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(1L, 1001L, 2L, 1002L, 3L, 1003L, 4L, 1004L, 5L, 1005L, 6L, 1006L);
        given(challengeRepository.save(any(Challenge.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(stepChallengeDetailRepository.existsByChallengeIdAndDeletedAtIsNull(any())).willReturn(false);

        initializer.run(null);

        then(challengeRepository).should(org.mockito.Mockito.times(6)).save(challengeCaptor.capture());
        then(stepChallengeDetailRepository).should(org.mockito.Mockito.times(6)).save(detailCaptor.capture());
        assertThat(challengeCaptor.getAllValues())
            .extracting("title")
            .containsExactly("서울-인천", "서울-천안", "서울-대전", "서울-대구", "서울-부산", "서울-제주");
        assertThat(detailCaptor.getAllValues())
            .extracting("destination", "targetStepCount", "displayOrder")
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("인천", 150_000, 1),
                org.assertj.core.groups.Tuple.tuple("천안", 200_000, 2),
                org.assertj.core.groups.Tuple.tuple("대전", 300_000, 3),
                org.assertj.core.groups.Tuple.tuple("대구", 400_000, 4),
                org.assertj.core.groups.Tuple.tuple("부산", 500_000, 5),
                org.assertj.core.groups.Tuple.tuple("제주", 700_000, 6)
            );
    }

    @Test
    @DisplayName("이미 detail이 있으면 중복 생성하지 않는다.")
    void skipExistingStepChallengeDetail() {
        StepChallengeMasterDataInitializer initializer = initializer(true);
        given(challengeRepository.findByChallengeTypeAndTitleAndDeletedAtIsNull(any(), any()))
            .willReturn(Optional.of(new Challenge(1L, ChallengeType.STEP, "서울-인천", "기존 챌린지")));
        given(stepChallengeDetailRepository.existsByChallengeIdAndDeletedAtIsNull(1L)).willReturn(true);

        initializer.run(null);

        then(challengeRepository).should(org.mockito.Mockito.never()).save(any(Challenge.class));
        then(stepChallengeDetailRepository).should(org.mockito.Mockito.never()).save(any(StepChallengeDetail.class));
        then(idGenerator).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("initializer가 비활성화되면 마스터 데이터를 생성하지 않는다.")
    void disabledInitializer() {
        StepChallengeMasterDataInitializer initializer = initializer(false);

        initializer.run(null);

        then(challengeRepository).shouldHaveNoInteractions();
        then(stepChallengeDetailRepository).shouldHaveNoInteractions();
    }

    private StepChallengeMasterDataInitializer initializer(boolean enabled) {
        return new StepChallengeMasterDataInitializer(
            idGenerator,
            challengeRepository,
            stepChallengeDetailRepository,
            enabled
        );
    }
}
