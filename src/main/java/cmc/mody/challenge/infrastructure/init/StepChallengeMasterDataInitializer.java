package cmc.mody.challenge.infrastructure.init;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.StepChallengeDetail;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.StepChallengeDetailRepository;
import cmc.mody.common.id.IdGenerator;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StepChallengeMasterDataInitializer implements ApplicationRunner {
    private static final String DEPARTURE = "서울";
    private static final List<StepChallengeSeed> SEEDS = List.of(
        new StepChallengeSeed("서울-인천", "인천", BigDecimal.valueOf(60), 150_000, 1),
        new StepChallengeSeed("서울-천안", "천안", BigDecimal.valueOf(90), 200_000, 2),
        new StepChallengeSeed("서울-대전", "대전", BigDecimal.valueOf(160), 300_000, 3),
        new StepChallengeSeed("서울-대구", "대구", BigDecimal.valueOf(240), 400_000, 4),
        new StepChallengeSeed("서울-부산", "부산", BigDecimal.valueOf(325), 500_000, 5),
        new StepChallengeSeed("서울-제주", "제주", BigDecimal.valueOf(500), 700_000, 6)
    );

    private final IdGenerator idGenerator;
    private final ChallengeRepository challengeRepository;
    private final StepChallengeDetailRepository stepChallengeDetailRepository;
    private final boolean enabled;

    public StepChallengeMasterDataInitializer(
        IdGenerator idGenerator,
        ChallengeRepository challengeRepository,
        StepChallengeDetailRepository stepChallengeDetailRepository,
        @Value("${challenge.step-master-data-initializer.enabled:true}") boolean enabled
    ) {
        this.idGenerator = idGenerator;
        this.challengeRepository = challengeRepository;
        this.stepChallengeDetailRepository = stepChallengeDetailRepository;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        SEEDS.forEach(this::upsert);
    }

    private void upsert(StepChallengeSeed seed) {
        Challenge challenge = challengeRepository
            .findByChallengeTypeAndTitleAndDeletedAtIsNull(ChallengeType.STEP, seed.title())
            .orElseGet(() -> challengeRepository.save(new Challenge(
                idGenerator.nextId(),
                ChallengeType.STEP,
                seed.title(),
                seed.description()
            )));

        if (stepChallengeDetailRepository.existsByChallengeIdAndDeletedAtIsNull(challenge.getId())) {
            return;
        }
        stepChallengeDetailRepository.save(new StepChallengeDetail(
            idGenerator.nextId(),
            challenge.getId(),
            DEPARTURE,
            seed.destination(),
            seed.distanceKm(),
            seed.targetStepCount(),
            seed.displayOrder()
        ));
    }

    private record StepChallengeSeed(
        String title,
        String destination,
        BigDecimal distanceKm,
        int targetStepCount,
        int displayOrder
    ) {
        private String description() {
            return DEPARTURE + "에서 " + destination + "까지 " + distanceKm.stripTrailingZeros().toPlainString()
                + "km를 걷는 챌린지입니다.";
        }
    }
}
