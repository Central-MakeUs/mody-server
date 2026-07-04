package cmc.mody.record.infrastructure.repository;

import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.record.domain.ActivityRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long> {
    @Query("""
        select record
        from ActivityRecord record
        where record.groupId = :groupId
          and record.deletedAt is null
          and record.uploadedAt >= :startAt
          and record.uploadedAt < :endAt
          and exists (
              select 1
              from GroupMember groupMember
              where groupMember.groupId = record.groupId
                and groupMember.memberId = record.memberId
                and groupMember.groupMemberStatus = :joinedStatus
                and groupMember.deletedAt is null
          )
        order by record.uploadedAt asc, record.id asc
        """)
    List<ActivityRecord> findActiveGroupRecordsBetween(
        @Param("groupId") Long groupId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("joinedStatus") GroupMemberStatus joinedStatus
    );

    @Query("""
        select record
        from ActivityRecord record
        where record.groupId = :groupId
          and record.deletedAt is null
          and record.uploadedAt >= :startAt
          and record.uploadedAt < :endAt
          and (:cursor is null or record.id < :cursor)
          and exists (
              select 1
              from GroupMember groupMember
              where groupMember.groupId = record.groupId
                and groupMember.memberId = record.memberId
                and groupMember.groupMemberStatus = :joinedStatus
                and groupMember.deletedAt is null
          )
        order by record.id desc
        """)
    List<ActivityRecord> findActiveGroupRecordsByCursor(
        @Param("groupId") Long groupId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("cursor") Long cursor,
        @Param("joinedStatus") GroupMemberStatus joinedStatus,
        Pageable pageable
    );

    @Query("""
        select record
        from ActivityRecord record
        where record.groupId = :groupId
          and record.memberId = :memberId
          and record.deletedAt is null
          and record.uploadedAt < :endAt
          and exists (
              select 1
              from GroupMember groupMember
              where groupMember.groupId = record.groupId
                and groupMember.memberId = record.memberId
                and groupMember.groupMemberStatus = :joinedStatus
                and groupMember.deletedAt is null
          )
        order by record.uploadedAt desc, record.id desc
        """)
    List<ActivityRecord> findActiveGroupRecordsByMemberBefore(
        @Param("groupId") Long groupId,
        @Param("memberId") Long memberId,
        @Param("endAt") LocalDateTime endAt,
        @Param("joinedStatus") GroupMemberStatus joinedStatus
    );

    @Query("""
        select count(record)
        from ActivityRecord record
        where record.groupId = :groupId
          and record.memberId = :memberId
          and record.deletedAt is null
          and record.uploadedAt > :after
          and exists (
              select 1
              from GroupMember groupMember
              where groupMember.groupId = record.groupId
                and groupMember.memberId = record.memberId
                and groupMember.groupMemberStatus = :joinedStatus
                and groupMember.deletedAt is null
          )
        """)
    long countActiveGroupRecordsAfter(
        @Param("groupId") Long groupId,
        @Param("memberId") Long memberId,
        @Param("after") LocalDateTime after,
        @Param("joinedStatus") GroupMemberStatus joinedStatus
    );

    @Query("""
        select record
        from ActivityRecord record
        where record.memberId = :memberId
          and record.deletedAt is null
          and record.uploadedAt >= :startAt
          and record.uploadedAt < :endAt
          and (:cursor is null or record.id > :cursor)
          and (
              (:groupId is null and record.groupId is null)
              or record.groupId = :groupId
          )
          and (
              :groupId is null
              or exists (
                  select 1
                  from GroupMember groupMember
                  where groupMember.groupId = record.groupId
                    and groupMember.memberId = record.memberId
                    and groupMember.groupMemberStatus = :joinedStatus
                    and groupMember.deletedAt is null
              )
          )
        order by record.id asc
        """)
    List<ActivityRecord> findActiveRecordsForDetailCarousel(
        @Param("groupId") Long groupId,
        @Param("memberId") Long memberId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("cursor") Long cursor,
        @Param("joinedStatus") GroupMemberStatus joinedStatus,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("""
        select count(record)
        from ActivityRecord record
        where record.memberId = :memberId
          and record.deletedAt is null
          and record.uploadedAt >= :startAt
          and record.uploadedAt < :endAt
          and (
              (:groupId is null and record.groupId is null)
              or record.groupId = :groupId
          )
          and (
              :groupId is null
              or exists (
                  select 1
                  from GroupMember groupMember
                  where groupMember.groupId = record.groupId
                    and groupMember.memberId = record.memberId
                    and groupMember.groupMemberStatus = :joinedStatus
                    and groupMember.deletedAt is null
              )
          )
        """)
    long countActiveRecordsForDetailCarousel(
        @Param("groupId") Long groupId,
        @Param("memberId") Long memberId,
        @Param("startAt") LocalDateTime startAt,
        @Param("endAt") LocalDateTime endAt,
        @Param("joinedStatus") GroupMemberStatus joinedStatus
    );

    List<ActivityRecord> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<ActivityRecord> findByMemberIdAndGroupIdAndDeletedAtIsNull(Long memberId, Long groupId);
}
