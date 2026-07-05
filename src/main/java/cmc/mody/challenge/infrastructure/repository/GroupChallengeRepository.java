package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupChallengeRepository extends JpaRepository<GroupChallenge, Long> {
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
}
