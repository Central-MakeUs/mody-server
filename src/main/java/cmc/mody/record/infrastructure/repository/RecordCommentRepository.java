package cmc.mody.record.infrastructure.repository;

import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.record.domain.RecordComment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordCommentRepository extends JpaRepository<RecordComment, Long> {
    List<RecordComment> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<RecordComment> findByRecordIdInAndDeletedAtIsNull(List<Long> recordIds);

    @Query("""
        select comment
        from RecordComment comment
        where comment.recordId = :recordId
          and comment.groupId = :groupId
          and comment.deletedAt is null
          and (:cursor is null or comment.id > :cursor)
          and exists (
              select 1
              from GroupMember groupMember
              where groupMember.groupId = :groupId
                and groupMember.memberId = comment.memberId
                and groupMember.groupMemberStatus = :joinedStatus
                and groupMember.deletedAt is null
          )
        order by comment.id asc
        """)
    List<RecordComment> findActiveCommentsByCursor(
        @Param("recordId") Long recordId,
        @Param("groupId") Long groupId,
        @Param("memberId") Long memberId,
        @Param("cursor") Long cursor,
        @Param("joinedStatus") GroupMemberStatus joinedStatus,
        Pageable pageable
    );

    @Query("""
        select comment
        from RecordComment comment
        where comment.memberId = :memberId
          and comment.groupId = :groupId
          and comment.deletedAt is null
        """)
    List<RecordComment> findActiveCommentsByMemberIdAndGroupId(
        @Param("memberId") Long memberId,
        @Param("groupId") Long groupId
    );

    List<RecordComment> findByRecordIdInAndGroupIdAndDeletedAtIsNull(List<Long> recordIds, Long groupId);
}
