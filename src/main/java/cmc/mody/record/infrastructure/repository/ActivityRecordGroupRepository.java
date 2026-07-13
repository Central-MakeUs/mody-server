package cmc.mody.record.infrastructure.repository;

import cmc.mody.record.domain.ActivityRecordGroup;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordGroupRepository extends JpaRepository<ActivityRecordGroup, Long> {
    Optional<ActivityRecordGroup> findByRecordIdAndGroupIdAndDeletedAtIsNull(Long recordId, Long groupId);

    List<ActivityRecordGroup> findByRecordIdAndDeletedAtIsNull(Long recordId);

    List<ActivityRecordGroup> findByRecordIdInAndDeletedAtIsNull(List<Long> recordIds);

    List<ActivityRecordGroup> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<ActivityRecordGroup> findByMemberIdAndGroupIdAndDeletedAtIsNull(Long memberId, Long groupId);
}
