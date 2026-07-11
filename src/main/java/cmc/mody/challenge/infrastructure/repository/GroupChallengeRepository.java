package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupChallengeRepository extends JpaRepository<GroupChallenge, Long> {
    List<GroupChallenge> findByGroupIdAndDeletedAtIsNull(Long groupId);

    Optional<GroupChallenge> findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
        Long groupId,
        Collection<Long> challengeIds,
        GroupChallengeStatus status
    );

    List<GroupChallenge> findByGroupIdAndChallengeIdInAndGroupChallengeStatusInAndDeletedAtIsNullOrderByEndedAtAscIdAsc(
        Long groupId,
        Collection<Long> challengeIds,
        Collection<GroupChallengeStatus> statuses
    );

    List<GroupChallenge>
        findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndStartsOnLessThanEqualAndEndsOnGreaterThanEqualAndDeletedAtIsNullOrderByEndsOnAscIdAsc(
            Long groupId,
            Collection<Long> challengeIds,
            GroupChallengeStatus status,
            LocalDate startsOn,
            LocalDate endsOn
        );

    Optional<GroupChallenge> findByIdAndGroupIdAndDeletedAtIsNull(Long id, Long groupId);

    Optional<GroupChallenge> findByIdAndDeletedAtIsNull(Long id);

    long countByGroupIdAndGroupChallengeStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanAndDeletedAtIsNull(
        Long groupId,
        GroupChallengeStatus status,
        LocalDateTime startAt,
        LocalDateTime endAt
    );
}
