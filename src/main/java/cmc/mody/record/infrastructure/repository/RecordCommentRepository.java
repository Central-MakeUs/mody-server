package cmc.mody.record.infrastructure.repository;

import cmc.mody.record.domain.RecordComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordCommentRepository extends JpaRepository<RecordComment, Long> {
    List<RecordComment> findByRecordIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Long recordId);
}
