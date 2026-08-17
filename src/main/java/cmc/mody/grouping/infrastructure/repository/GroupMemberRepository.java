package cmc.mody.grouping.infrastructure.repository;

import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    long countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(Long memberId, GroupMemberStatus status);

    long countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(Long groupId, GroupMemberStatus status);

    boolean existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
        Long memberId,
        Long groupId,
        GroupMemberStatus status
    );

    Optional<GroupMember> findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
        Long memberId,
        Long groupId,
        GroupMemberStatus status
    );

    List<GroupMember> findByMemberIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
        Long memberId,
        GroupMemberStatus status
    );

    List<GroupMember> findByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
        Long memberId,
        GroupMemberStatus status
    );

    List<GroupMember> findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
        Long groupId,
        GroupMemberStatus status
    );

    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(Long groupId);

    List<GroupMember> findByGroupMemberStatusAndDeletedAtIsNull(GroupMemberStatus status, Pageable pageable);

    @Query("""
        select groupMember.groupId as groupId, count(groupMember) as memberCount
        from GroupMember groupMember
        where groupMember.groupId in :groupIds
          and groupMember.groupMemberStatus = :status
          and groupMember.deletedAt is null
        group by groupMember.groupId
        """)
    List<GroupMemberCount> countByGroupIdInAndStatus(
        @Param("groupIds") List<Long> groupIds,
        @Param("status") GroupMemberStatus status
    );

    interface GroupMemberCount {
        Long getGroupId();

        long getMemberCount();
    }
}
