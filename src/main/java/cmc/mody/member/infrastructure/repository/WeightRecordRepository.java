package cmc.mody.member.infrastructure.repository;

import cmc.mody.member.domain.WeightRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    Optional<WeightRecord> findTopByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(Long memberId);

    List<WeightRecord> findByMemberIdAndDeletedAtIsNullOrderByRecordedOnDescCreatedAtDesc(Long memberId);

    List<WeightRecord> findByMemberIdAndDeletedAtIsNull(Long memberId);
}
