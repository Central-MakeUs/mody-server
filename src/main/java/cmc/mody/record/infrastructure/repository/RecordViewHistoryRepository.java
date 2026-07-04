package cmc.mody.record.infrastructure.repository;

import cmc.mody.record.domain.RecordViewHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordViewHistoryRepository extends JpaRepository<RecordViewHistory, Long> {
    Optional<RecordViewHistory> findByViewerMemberIdAndGroupIdAndWriterMemberIdAndDeletedAtIsNull(
        Long viewerMemberId,
        Long groupId,
        Long writerMemberId
    );
}
