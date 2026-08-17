package cmc.mody.grouping.application;

import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminGroupService {
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional(readOnly = true)
    public AdminGroupListResult getGroups() {
        List<ModyGroup> groups = modyGroupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        if (groups.isEmpty()) {
            return new AdminGroupListResult(List.of());
        }

        Map<Long, Long> memberCountByGroupId = groupMemberRepository.countByGroupIdInAndStatus(
                groups.stream().map(ModyGroup::getId).toList(),
                GroupMemberStatus.JOINED
            ).stream()
            .collect(java.util.stream.Collectors.toMap(
                GroupMemberRepository.GroupMemberCount::getGroupId,
                GroupMemberRepository.GroupMemberCount::getMemberCount
            ));

        return new AdminGroupListResult(groups.stream()
            .map(group -> new AdminGroupResult(
                group.getId(),
                group.getName(),
                group.getCode(),
                memberCountByGroupId.getOrDefault(group.getId(), 0L)
            ))
            .toList());
    }

    @Transactional(readOnly = true)
    public AdminGroupDetailResult getGroup(Long groupId) {
        ModyGroup group = getActiveGroup(groupId);
        List<AdminGroupMemberResult> members = groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .map(member -> new AdminGroupMemberResult(
                member.getMemberId(),
                member.getDisplayNickname(),
                member.getDisplayProfileImageKey(),
                member.getJoinedAt()
            ))
            .toList();
        return new AdminGroupDetailResult(group.getId(), group.getName(), group.getCode(), members);
    }

    @Transactional
    public AdminGroupResult updateGroupName(Long groupId, String name) {
        ModyGroup group = getActiveGroup(groupId);
        group.updateName(name.trim());
        long memberCount = groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            groupId,
            GroupMemberStatus.JOINED
        );
        return new AdminGroupResult(group.getId(), group.getName(), group.getCode(), memberCount);
    }

    private ModyGroup getActiveGroup(Long groupId) {
        return modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new cmc.mody.common.api.exception.GeneralException(
                cmc.mody.common.api.status.ErrorStatus.GROUP_NOT_FOUND
            ));
    }

    public record AdminGroupListResult(List<AdminGroupResult> groups) {
    }

    public record AdminGroupResult(Long groupId, String name, String code, long memberCount) {
    }

    public record AdminGroupDetailResult(Long groupId, String name, String code, List<AdminGroupMemberResult> members) {
    }

    public record AdminGroupMemberResult(Long memberId, String nickname, String profileImageKey, java.time.LocalDateTime joinedAt) {
    }
}
