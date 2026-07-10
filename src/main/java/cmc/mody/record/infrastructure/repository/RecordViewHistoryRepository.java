package cmc.mody.record.infrastructure.repository;

import cmc.mody.record.domain.RecordViewHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordViewHistoryRepository extends JpaRepository<RecordViewHistory, Long> {
    Optional<RecordViewHistory> findByViewerMemberIdAndGroupIdAndWriterMemberIdAndDeletedAtIsNull(
        Long viewerMemberId,
        Long groupId,
        Long writerMemberId
    );

    @Query("""
        select history
        from RecordViewHistory history
        where history.deletedAt is null
          and (
            history.viewerMemberId = :memberId
            or history.writerMemberId = :memberId
          )
        """)
    List<RecordViewHistory> findActiveByMemberId(@Param("memberId") Long memberId);

    @Query("""
        select history
        from RecordViewHistory history
        where history.deletedAt is null
          and history.groupId = :groupId
          and (
            history.viewerMemberId = :memberId
            or history.writerMemberId = :memberId
          )
        """)
    List<RecordViewHistory> findActiveByMemberIdAndGroupId(
        @Param("memberId") Long memberId,
        @Param("groupId") Long groupId
    );
}
