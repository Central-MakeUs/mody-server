package cmc.mody.grouping.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminGroupServiceTest {
    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private AdminGroupService adminGroupService;

    @Test
    void 활성_그룹과_가입_회원_수를_조회한다() {
        GroupMemberRepository.GroupMemberCount firstCount = memberCount(1L, 3L);
        GroupMemberRepository.GroupMemberCount secondCount = memberCount(2L, 1L);
        given(modyGroupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(List.of(
            new ModyGroup(1L, "GROUP001", "모디 크루"),
            new ModyGroup(2L, "GROUP002", "아침 러너")
        ));
        given(groupMemberRepository.countByGroupIdInAndStatus(List.of(1L, 2L), GroupMemberStatus.JOINED))
            .willReturn(List.of(firstCount, secondCount));

        AdminGroupService.AdminGroupListResult result = adminGroupService.getGroups();

        assertThat(result.groups()).containsExactly(
            new AdminGroupService.AdminGroupResult(1L, "모디 크루", "GROUP001", 3L),
            new AdminGroupService.AdminGroupResult(2L, "아침 러너", "GROUP002", 1L)
        );
    }

    @Test
    void 그룹이_없으면_회원_수를_조회하지_않는다() {
        given(modyGroupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(List.of());

        AdminGroupService.AdminGroupListResult result = adminGroupService.getGroups();

        assertThat(result.groups()).isEmpty();
        verifyNoInteractions(groupMemberRepository);
    }

    private GroupMemberRepository.GroupMemberCount memberCount(Long groupId, long memberCount) {
        GroupMemberRepository.GroupMemberCount count = mock(GroupMemberRepository.GroupMemberCount.class);
        given(count.getGroupId()).willReturn(groupId);
        given(count.getMemberCount()).willReturn(memberCount);
        return count;
    }
}
