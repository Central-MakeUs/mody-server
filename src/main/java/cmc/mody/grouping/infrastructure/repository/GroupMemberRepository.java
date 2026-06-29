package cmc.mody.grouping.infrastructure.repository;

import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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

    List<GroupMember> findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
        Long groupId,
        GroupMemberStatus status
    );
}
