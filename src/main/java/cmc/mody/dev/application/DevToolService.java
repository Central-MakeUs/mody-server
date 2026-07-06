package cmc.mody.dev.application;

import cmc.mody.auth.application.oauth.RefreshTokenService;
import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
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
import cmc.mody.notification.application.PushNotificationClient;
import cmc.mody.notification.application.PushNotificationResult;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.fcm.FcmProperties;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile({"local", "dev"})
@Service
@RequiredArgsConstructor
public class DevToolService {
    private final IdGenerator idGenerator;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final NotificationRepository notificationRepository;
    private final PushNotificationClient pushNotificationClient;
    private final FcmProperties fcmProperties;

    @Transactional
    public MockMemberResult createMockMember(CreateMockMemberCommand command) {
        Long memberId = idGenerator.nextId();
        String nickname = resolveNickname(memberId, command.nickname());
        LocalDate birthDate = resolveBirthDate(command);
        BigDecimal targetWeightKg = resolveTargetWeight(command);

        Member member = new Member(memberId, nickname, birthDate, targetWeightKg);
        if (command.groupOnboardingCompleted()) {
            member.completeGroupOnboarding();
        }
        Member savedMember = memberRepository.save(member);
        return MockMemberResult.from(savedMember, 0);
    }

    @Transactional(readOnly = true)
    public MemberListResult getMembers() {
        List<MockMemberResult> members = memberRepository.findAll()
            .stream()
            .filter(Member::isActive)
            .map(member -> MockMemberResult.from(
                member,
                countJoinedGroups(member.getId())
            ))
            .toList();
        return new MemberListResult(members);
    }

    @Transactional
    public DevTokenResult issueToken(IssueTokenCommand command) {
        Member member = validateMember(command.memberId());
        TokenDto token = tokenProvider.createToken(member.getId());
        refreshTokenService.replace(member.getId(), token.refreshToken());
        DevMeResult memberState = getMe(member.getId());
        return new DevTokenResult(
            member.getId(),
            token.accessToken(),
            token.refreshToken(),
            memberState.personalInfoCompleted(),
            memberState.groupOnboardingCompleted(),
            memberState.mainAccessible(),
            memberState.joinedGroupCount()
        );
    }

    public TestPushResult sendTestPush(TestPushCommand command) {
        Notification notification = new Notification(
            idGenerator.nextId(),
            0L,
            NotificationType.DEV_TEST,
            command.title(),
            command.body()
        );
        PushNotificationResult result = pushNotificationClient.send(notification, List.of(command.fcmToken()));
        return new TestPushResult(
            fcmProperties.isEnabled(),
            result.invalidTokens().contains(command.fcmToken())
        );
    }

    @Transactional(readOnly = true)
    public DevGroupListResult getGroups() {
        List<DevGroupSummaryResult> groups = modyGroupRepository.findAll()
            .stream()
            .filter(ModyGroup::isActive)
            .map(group -> new DevGroupSummaryResult(
                group.getId(),
                group.getName(),
                group.getCode(),
                countJoinedMembers(group.getId())
            ))
            .toList();
        return new DevGroupListResult(groups);
    }

    @Transactional(readOnly = true)
    public DevGroupDetailResult getGroup(Long groupId) {
        ModyGroup group = validateGroup(groupId);
        List<GroupMember> memberships = groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED);
        Map<Long, Member> membersById = memberRepository.findAllById(memberships.stream()
                .map(GroupMember::getMemberId)
                .toList())
            .stream()
            .filter(Member::isActive)
            .collect(Collectors.toMap(Member::getId, Function.identity()));
        List<DevGroupMemberResult> members = memberships.stream()
            .map(groupMember -> toGroupMemberResult(groupMember, membersById.get(groupMember.getMemberId())))
            .filter(Objects::nonNull)
            .toList();

        return new DevGroupDetailResult(
            group.getId(),
            group.getName(),
            group.getCode(),
            members.size(),
            members
        );
    }

    @Transactional
    public InboxNotificationResult createInboxNotification(Long memberId, InboxNotificationCommand command) {
        validateMember(memberId);
        Notification notification = notificationRepository.save(new Notification(
            idGenerator.nextId(),
            memberId,
            NotificationType.DEV_TEST,
            command.title(),
            command.body()
        ));
        return new InboxNotificationResult(notification.getId());
    }

    @Transactional(readOnly = true)
    public DevMeResult getMe(Long memberId) {
        Member member = validateMember(memberId);
        List<GroupMember> memberships = groupMemberRepository
            .findByMemberIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(memberId, GroupMemberStatus.JOINED);
        Map<Long, ModyGroup> groupsById = modyGroupRepository.findAllById(memberships.stream()
                .map(GroupMember::getGroupId)
                .toList())
            .stream()
            .filter(ModyGroup::isActive)
            .collect(Collectors.toMap(ModyGroup::getId, Function.identity()));
        List<DevGroupResult> groups = memberships.stream()
            .map(groupMember -> groupsById.get(groupMember.getGroupId()))
            .filter(group -> group != null)
            .map(group -> new DevGroupResult(group.getId(), group.getName(), group.getCode()))
            .toList();
        return new DevMeResult(
            member.getId(),
            member.getNickname(),
            member.isPersonalInfoCompleted(),
            member.isGroupOnboardingCompleted(),
            member.isPersonalInfoCompleted() && !groups.isEmpty(),
            groups.size(),
            groups
        );
    }

    private Member validateMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private ModyGroup validateGroup(Long groupId) {
        return modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
    }

    private long countJoinedGroups(Long memberId) {
        return groupMemberRepository.countByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            GroupMemberStatus.JOINED
        );
    }

    private long countJoinedMembers(Long groupId) {
        return groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            groupId,
            GroupMemberStatus.JOINED
        );
    }

    private String resolveNickname(Long memberId, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            String suffix = String.valueOf(memberId);
            return "mock-" + suffix.substring(Math.max(0, suffix.length() - 6));
        }
        return nickname.strip();
    }

    private LocalDate resolveBirthDate(CreateMockMemberCommand command) {
        if (!command.personalInfoCompleted()) {
            return command.birthDate();
        }
        return command.birthDate() == null ? LocalDate.of(2000, 1, 1) : command.birthDate();
    }

    private BigDecimal resolveTargetWeight(CreateMockMemberCommand command) {
        if (!command.personalInfoCompleted()) {
            return command.targetWeightKg();
        }
        return command.targetWeightKg() == null ? BigDecimal.valueOf(60.0) : command.targetWeightKg();
    }

    private DevGroupMemberResult toGroupMemberResult(GroupMember groupMember, Member member) {
        if (member == null) {
            return null;
        }
        String nickname = groupMember.getDisplayNickname() == null
            ? member.getNickname()
            : groupMember.getDisplayNickname();
        String profileImageKey = groupMember.getDisplayProfileImageKey() == null
            ? member.getProfileImageKey()
            : groupMember.getDisplayProfileImageKey();
        return new DevGroupMemberResult(
            member.getId(),
            nickname,
            profileImageKey,
            groupMember.getJoinedAt()
        );
    }

    public record CreateMockMemberCommand(
        String nickname,
        LocalDate birthDate,
        BigDecimal targetWeightKg,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted
    ) {
    }

    public record MockMemberResult(
        Long memberId,
        String nickname,
        LocalDate birthDate,
        BigDecimal targetWeightKg,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted,
        long joinedGroupCount
    ) {
        static MockMemberResult from(Member member, long joinedGroupCount) {
            return new MockMemberResult(
                member.getId(),
                member.getNickname(),
                member.getBirthDate(),
                member.getTargetWeightKg(),
                member.isPersonalInfoCompleted(),
                member.isGroupOnboardingCompleted(),
                joinedGroupCount
            );
        }
    }

    public record MemberListResult(List<MockMemberResult> members) {
    }

    public record IssueTokenCommand(Long memberId) {
    }

    public record DevTokenResult(
        Long memberId,
        String accessToken,
        String refreshToken,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted,
        boolean mainAccessible,
        int joinedGroupCount
    ) {
    }

    public record TestPushCommand(String fcmToken, String title, String body) {
    }

    public record TestPushResult(boolean fcmEnabled, boolean invalidToken) {
    }

    public record InboxNotificationCommand(String title, String body) {
    }

    public record InboxNotificationResult(Long notificationId) {
    }

    public record DevMeResult(
        Long memberId,
        String nickname,
        boolean personalInfoCompleted,
        boolean groupOnboardingCompleted,
        boolean mainAccessible,
        int joinedGroupCount,
        List<DevGroupResult> groups
    ) {
    }

    public record DevGroupResult(Long groupId, String name, String code) {
    }

    public record DevGroupListResult(List<DevGroupSummaryResult> groups) {
    }

    public record DevGroupSummaryResult(Long groupId, String name, String code, long memberCount) {
    }

    public record DevGroupDetailResult(
        Long groupId,
        String name,
        String code,
        int memberCount,
        List<DevGroupMemberResult> members
    ) {
    }

    public record DevGroupMemberResult(
        Long memberId,
        String nickname,
        String profileImageKey,
        java.time.LocalDateTime joinedAt
    ) {
    }
}
