package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.GlobalWeeklyChallenge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalWeeklyChallengeRepository extends JpaRepository<GlobalWeeklyChallenge, Long> {
    Optional<GlobalWeeklyChallenge> findByIdAndDeletedAtIsNull(Long id);

    Optional<GlobalWeeklyChallenge> findByChallengeIdAndDeletedAtIsNull(Long challengeId);

    Optional<GlobalWeeklyChallenge> findByIdempotencyKeyAndDeletedAtIsNull(String idempotencyKey);

    List<GlobalWeeklyChallenge> findByDeletedAtIsNullOrderByStartsOnDescIdDesc();

    List<GlobalWeeklyChallenge>
        findByStartsOnLessThanEqualAndEndsOnGreaterThanEqualAndDeletedAtIsNull(LocalDate startsOn, LocalDate endsOn);
}
