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

    public record AdminGroupListResult(List<AdminGroupResult> groups) {
    }

    public record AdminGroupResult(Long groupId, String name, String code, long memberCount) {
    }
}
