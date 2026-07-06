package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.StepChallengeDetail;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepChallengeDetailRepository extends JpaRepository<StepChallengeDetail, Long> {
    boolean existsByChallengeIdAndDeletedAtIsNull(Long challengeId);

    Optional<StepChallengeDetail> findByChallengeIdAndDeletedAtIsNull(Long challengeId);

    List<StepChallengeDetail> findByChallengeIdInAndDeletedAtIsNull(List<Long> challengeIds);
}
