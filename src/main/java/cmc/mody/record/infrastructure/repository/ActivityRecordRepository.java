package cmc.mody.record.infrastructure.repository;

import cmc.mody.record.domain.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {
}
