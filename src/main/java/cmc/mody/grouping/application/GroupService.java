package cmc.mody.grouping.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationRequestService;
import cmc.mody.record.domain.RecordViewHistory;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordViewHistoryRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {
    private static final int MAX_GROUP_COUNT = 4;
    private static final int MAX_GROUP_MEMBER_COUNT = 12;
    private static final int GROUP_CODE_LENGTH = 6;
    private static final int MAX_GROUP_CODE_GENERATION_ATTEMPTS = 20;
    private static final char[] GROUP_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationRequestService notificationRequestService;
    private final ActivityRecordRepository activityRecordRepository;
    private final RecordViewHistoryRepository recordViewHistoryRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public GroupCodeResult generateCode(Long memberId) {
        getMember(memberId);
        return new GroupCodeResult(generateUniqueCode());
    }

    @Transactional
    public GroupCreateResult createGroup(Long memberId, GroupCreateCommand command) {
        Member member = getMember(memberId);
        validateJoinable(memberId);

        ModyGroup group = modyGroupRepository.save(new ModyGroup(
            idGenerator.nextId(),
            generateUniqueCode(),
            command.name()
        ));
        groupMemberRepository.save(newGroupMember(member, group.getId()));
        member.completeGroupOnboarding();

        return new GroupCreateResult(group.getId(), group.getCode(), group.getName());
    }

    @Transactional
    public GroupJoinResult joinGroup(Long memberId, GroupJoinCommand command) {
        Member member = getMember(memberId);
        ModyGroup group = modyGroupRepository.findByCodeAndDeletedAtIsNull(command.code())
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        if (groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            group.getId(),
            GroupMemberStatus.JOINED
        )) {
            throw new GeneralException(ErrorStatus.GROUP_ALREADY_JOINED);
        }
        validateJoinable(memberId);
        validateGroupCapacity(group.getId());

        groupMemberRepository.save(newGroupMember(member, group.getId()));
        member.completeGroupOnboarding();
        int memberCount = (int) groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            group.getId(),
            GroupMemberStatus.JOINED
        );
        notificationRequestService.requestGroupMemberJoined(
            group.getId(),
            group.getName(),
            member.getId(),
            member.getNickname()
        );
        return new GroupJoinResult(group.getId(), group.getCode(), group.getName(), memberCount);
    }

    @Transactional(readOnly = true)
    public GroupListResult getMyGroups(Long memberId) {
        getMember(memberId);
        List<GroupSummaryResult> groups = groupMemberRepository
            .findByMemberIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(memberId, GroupMemberStatus.JOINED)
            .stream()
            .map(this::toGroupSummary)
            .toList();
        return new GroupListResult(groups);
    }

    @Transactional(readOnly = true)
    public GroupMemberListResult getGroupMembers(Long memberId, Long groupId) {
        validateJoinedMember(memberId, groupId);
        List<GroupMemberResult> members = groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .map(groupMember -> new GroupMemberResult(
                groupMember.getMemberId(),
                groupMember.getDisplayNickname(),
                groupMember.getDisplayProfileImageKey(),
                countUnreadRecords(memberId, groupId, groupMember.getMemberId())
            ))
            .toList();
        return new GroupMemberListResult(members);
    }

    @Transactional
    public void leaveGroup(Long memberId, Long groupId) {
        GroupMember groupMember = groupMemberRepository
            .findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(memberId, groupId, GroupMemberStatus.JOINED)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
        groupMember.leave(LocalDateTime.now());
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private void validateJoinable(Long memberId) {
        long joinedGroupCount = groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            GroupMemberStatus.JOINED
        );
        if (joinedGroupCount >= MAX_GROUP_COUNT) {
            throw new GeneralException(ErrorStatus.GROUP_LIMIT_EXCEEDED);
        }
    }

    private void validateGroupCapacity(Long groupId) {
        long memberCount = groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            groupId,
            GroupMemberStatus.JOINED
        );
        if (memberCount >= MAX_GROUP_MEMBER_COUNT) {
            throw new GeneralException(ErrorStatus.GROUP_CAPACITY_EXCEEDED);
        }
    }

    private void validateJoinedMember(Long memberId, Long groupId) {
        boolean groupExists = modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .isPresent();
        if (!groupExists) {
            throw new GeneralException(ErrorStatus.GROUP_NOT_FOUND);
        }
        boolean joined = groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            groupId,
            GroupMemberStatus.JOINED
        );
        if (!joined) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
    }

    private GroupSummaryResult toGroupSummary(GroupMember groupMember) {
        ModyGroup group = modyGroupRepository.findById(groupMember.getGroupId())
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        int memberCount = (int) groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            group.getId(),
            GroupMemberStatus.JOINED
        );
        return new GroupSummaryResult(group.getId(), group.getName(), group.getCode(), memberCount);
    }

    private GroupMember newGroupMember(Member member, Long groupId) {
        return new GroupMember(
            idGenerator.nextId(),
            member.getId(),
            groupId,
            member.getNickname(),
            member.getProfileImageKey(),
            LocalDateTime.now()
        );
    }

    private int countUnreadRecords(Long viewerMemberId, Long groupId, Long writerMemberId) {
        if (viewerMemberId.equals(writerMemberId)) {
            return 0;
        }
        LocalDateTime lastViewedAt = recordViewHistoryRepository
            .findByViewerMemberIdAndGroupIdAndWriterMemberIdAndDeletedAtIsNull(
                viewerMemberId,
                groupId,
                writerMemberId
            )
            .map(RecordViewHistory::getLastViewedAt)
            .orElse(LocalDateTime.MIN);
        long unreadCount = activityRecordRepository.countActiveGroupRecordsAfter(
            groupId,
            writerMemberId,
            lastViewedAt,
            GroupMemberStatus.JOINED
        );
        return Math.toIntExact(unreadCount);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GROUP_CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (!modyGroupRepository.existsByCodeAndDeletedAtIsNull(code)) {
                return code;
            }
        }
        throw new GeneralException(ErrorStatus.GROUP_CODE_GENERATION_FAILED);
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(GROUP_CODE_LENGTH);
        for (int index = 0; index < GROUP_CODE_LENGTH; index++) {
            builder.append(GROUP_CODE_CHARS[secureRandom.nextInt(GROUP_CODE_CHARS.length)]);
        }
        return builder.toString();
    }

    public record GroupCodeResult(String code) {
    }

    public record GroupCreateCommand(String name) {
    }

    public record GroupCreateResult(Long groupId, String code, String name) {
    }

    public record GroupJoinCommand(String code) {
    }

    public record GroupJoinResult(Long groupId, String code, String name, int memberCount) {
    }

    public record GroupListResult(List<GroupSummaryResult> groups) {
    }

    public record GroupSummaryResult(Long groupId, String name, String code, int memberCount) {
    }

    public record GroupMemberListResult(List<GroupMemberResult> members) {
    }

    public record GroupMemberResult(Long memberId, String nickname, String profileImageUrl, int unreadRecordCount) {
    }
}
