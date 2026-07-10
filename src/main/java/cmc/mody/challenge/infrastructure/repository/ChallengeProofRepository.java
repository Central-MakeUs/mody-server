package cmc.mody.challenge.infrastructure.repository;

import cmc.mody.challenge.domain.ChallengeProof;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeProofRepository extends JpaRepository<ChallengeProof, Long> {
    boolean existsByGroupChallengeIdAndMemberIdAndDeletedAtIsNull(Long groupChallengeId, Long memberId);

    List<ChallengeProof> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<ChallengeProof> findByMemberIdAndGroupChallengeIdInAndDeletedAtIsNull(
        Long memberId,
        Collection<Long> groupChallengeIds
    );

    long countByGroupChallengeIdAndDeletedAtIsNull(Long groupChallengeId);

    List<ChallengeProof> findByGroupChallengeIdInAndDeletedAtIsNullOrderByUploadedAtAscIdAsc(
        Collection<Long> groupChallengeIds
    );

    List<ChallengeProof> findByGroupChallengeIdAndDeletedAtIsNullOrderByUploadedAtAscIdAsc(Long groupChallengeId);
}
