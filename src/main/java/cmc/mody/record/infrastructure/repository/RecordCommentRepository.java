package cmc.mody.record.infrastructure.repository;

import cmc.mody.record.domain.RecordComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordCommentRepository extends JpaRepository<RecordComment, Long> {
    List<RecordComment> findByRecordIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Long recordId);

    List<RecordComment> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<RecordComment> findByRecordIdInAndDeletedAtIsNull(List<Long> recordIds);

    @Query("""
        select comment
        from RecordComment comment
        where comment.memberId = :memberId
          and comment.deletedAt is null
          and exists (
              select 1
              from ActivityRecord record
              where record.id = comment.recordId
                and record.groupId = :groupId
          )
        """)
    List<RecordComment> findActiveCommentsByMemberIdAndGroupId(
        @Param("memberId") Long memberId,
        @Param("groupId") Long groupId
    );
}
