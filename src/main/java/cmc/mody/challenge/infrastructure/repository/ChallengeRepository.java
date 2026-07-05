package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    Optional<Challenge> findByIdAndChallengeTypeAndDeletedAtIsNull(Long id, ChallengeType challengeType);

    Optional<Challenge> findByChallengeTypeAndTitleAndDeletedAtIsNull(ChallengeType challengeType, String title);

    List<Challenge> findByChallengeTypeAndDeletedAtIsNull(ChallengeType challengeType);
}
