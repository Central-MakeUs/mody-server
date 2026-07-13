package cmc.mody.grouping.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.domain.Status;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.application.GroupService.GroupCreateCommand;
import cmc.mody.grouping.application.GroupService.GroupCreateResult;
import cmc.mody.grouping.application.GroupService.GroupJoinCommand;
import cmc.mody.grouping.application.GroupService.GroupJoinResult;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationRequestService;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.domain.RecordViewHistory;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import cmc.mody.record.infrastructure.repository.RecordViewHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private NotificationRequestService notificationRequestService;

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private ActivityRecordGroupRepository activityRecordGroupRepository;

    @Mock
    private RecordCommentRepository recordCommentRepository;

    @Mock
    private RecordViewHistoryRepository recordViewHistoryRepository;

    @Captor
    private ArgumentCaptor<ModyGroup> groupCaptor;

    @Captor
    private ArgumentCaptor<GroupMember> groupMemberCaptor;

    @Test
    @DisplayName("그룹을 생성하면 생성자가 그룹 구성원으로 저장된다.")
    void createGroup() {
        GroupService service = service();
        Member member = member();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(1L, GroupMemberStatus.JOINED))
            .willReturn(0L);
        given(modyGroupRepository.existsByCodeAndDeletedAtIsNull(any(String.class))).willReturn(false);
        given(idGenerator.nextId()).willReturn(10L, 20L);
        given(modyGroupRepository.save(any(ModyGroup.class))).willAnswer(invocation -> invocation.getArgument(0));

        GroupCreateResult result = service.createGroup(1L, new GroupCreateCommand("모디 그룹"));

        assertThat(result.groupId()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("모디 그룹");
        assertThat(result.code()).hasSize(8);
        then(modyGroupRepository).should().save(groupCaptor.capture());
        then(groupMemberRepository).should().save(groupMemberCaptor.capture());
        assertThat(groupCaptor.getValue().getCode()).isEqualTo(result.code());
        assertThat(groupMemberCaptor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(groupMemberCaptor.getValue().getGroupId()).isEqualTo(10L);
        assertThat(groupMemberCaptor.getValue().getDisplayNickname()).isEqualTo("민석");
        assertThat(member.isGroupOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("그룹 코드로 참여하면 참여 인원을 반환한다.")
    void joinGroup() {
        GroupService service = service();
        Member member = member();
        ModyGroup group = new ModyGroup(10L, "ABCD2345", "모디 그룹");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(modyGroupRepository.findByCodeAndDeletedAtIsNull("ABCD2345")).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(false);
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(1L, GroupMemberStatus.JOINED))
            .willReturn(1L);
        given(groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(10L, GroupMemberStatus.JOINED))
            .willReturn(3L);
        given(idGenerator.nextId()).willReturn(20L);

        GroupJoinResult result = service.joinGroup(1L, new GroupJoinCommand("ABCD2345"));

        assertThat(result).isEqualTo(new GroupJoinResult(10L, "ABCD2345", "모디 그룹", 3));
        then(groupMemberRepository).should().save(groupMemberCaptor.capture());
        assertThat(groupMemberCaptor.getValue().getGroupId()).isEqualTo(10L);
        assertThat(member.isGroupOnboardingCompleted()).isTrue();
        then(notificationRequestService).should()
            .requestGroupMemberJoined(10L, "모디 그룹", 1L, "민석");
    }

    @Test
    @DisplayName("이미 참여 중인 그룹은 다시 참여할 수 없다.")
    void joinAlreadyJoinedGroup() {
        GroupService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findByCodeAndDeletedAtIsNull("ABCD2345"))
            .willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디 그룹")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);

        assertThatThrownBy(() -> service.joinGroup(1L, new GroupJoinCommand("ABCD2345")))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.GROUP_ALREADY_JOINED));
    }

    @Test
    @DisplayName("참여 그룹이 4개면 새 그룹에 참여할 수 없다.")
    void joinGroupLimitExceeded() {
        GroupService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findByCodeAndDeletedAtIsNull("ABCD2345"))
            .willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디 그룹")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(false);
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(1L, GroupMemberStatus.JOINED))
            .willReturn(4L);

        assertThatThrownBy(() -> service.joinGroup(1L, new GroupJoinCommand("ABCD2345")))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.GROUP_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("그룹 참여 인원이 12명이면 새 회원은 참여할 수 없다.")
    void joinGroupCapacityExceeded() {
        GroupService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findByCodeAndDeletedAtIsNull("ABCD2345"))
            .willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디 그룹")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(false);
        given(groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(1L, GroupMemberStatus.JOINED))
            .willReturn(1L);
        given(groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(10L, GroupMemberStatus.JOINED))
            .willReturn(12L);

        assertThatThrownBy(() -> service.joinGroup(1L, new GroupJoinCommand("ABCD2345")))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.GROUP_CAPACITY_EXCEEDED));
    }

    @Test
    @DisplayName("그룹 나가기는 참여 정보를 논리 삭제한다.")
    void leaveGroup() {
        GroupService service = service();
        GroupMember groupMember = new GroupMember(20L, 1L, 10L, LocalDateTime.now());
        ActivityRecordGroup recordGroup = new ActivityRecordGroup(30L, 100L, 10L, 1L, LocalDateTime.now());
        RecordComment recordComment = new RecordComment(40L, 100L, 10L, 2L, "기록 댓글");
        RecordComment myComment = new RecordComment(41L, 200L, 10L, 1L, "내 댓글");
        given(groupMemberRepository.findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(Optional.of(groupMember));
        given(activityRecordGroupRepository.findByMemberIdAndGroupIdAndDeletedAtIsNull(1L, 10L))
            .willReturn(List.of(recordGroup));
        given(recordCommentRepository.findByRecordIdInAndGroupIdAndDeletedAtIsNull(List.of(100L), 10L))
            .willReturn(List.of(recordComment));
        given(recordCommentRepository.findActiveCommentsByMemberIdAndGroupId(1L, 10L))
            .willReturn(List.of(myComment));

        service.leaveGroup(1L, 10L);

        assertThat(groupMember.getGroupMemberStatus()).isEqualTo(GroupMemberStatus.LEFT);
        assertThat(recordGroup.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(recordComment.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(myComment.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(groupMember.getLeftAt()).isNotNull();
        assertThat(groupMember.getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(groupMember.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("그룹원 조회는 마지막 상세 진입 이후 올라온 미확인 기록 수를 반환한다.")
    void getGroupMembersWithUnreadRecordCount() {
        GroupService service = service();
        LocalDateTime lastViewedAt = LocalDateTime.of(2026, 7, 1, 12, 0);
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디 그룹")));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            new GroupMember(20L, 1L, 10L, "민석", "profiles/member-1.jpg", LocalDateTime.of(2026, 6, 1, 0, 0)),
            new GroupMember(21L, 2L, 10L, "친구", "profiles/member-2.jpg", LocalDateTime.of(2026, 6, 2, 0, 0))
        ));
        given(recordViewHistoryRepository.findByViewerMemberIdAndGroupIdAndWriterMemberIdAndDeletedAtIsNull(
            1L,
            10L,
            2L
        )).willReturn(Optional.of(new RecordViewHistory(30L, 1L, 10L, 2L, lastViewedAt)));
        given(activityRecordRepository.countActiveGroupRecordsAfter(
            10L,
            2L,
            lastViewedAt,
            GroupMemberStatus.JOINED
        )).willReturn(3L);

        GroupService.GroupMemberListResult result = service.getGroupMembers(1L, 10L);

        assertThat(result.members()).hasSize(2);
        assertThat(result.members().get(0).unreadRecordCount()).isZero();
        assertThat(result.members().get(1).unreadRecordCount()).isEqualTo(3);
    }

    private GroupService service() {
        return new GroupService(
            idGenerator,
            memberRepository,
            modyGroupRepository,
            groupMemberRepository,
            notificationRequestService,
            activityRecordRepository,
            activityRecordGroupRepository,
            recordCommentRepository,
            recordViewHistoryRepository
        );
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }
}
