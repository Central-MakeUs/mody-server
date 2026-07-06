package cmc.mody.challenge.application;

import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.upload.UploadProperties;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationRequestService;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChallengeHomeService {
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final NotificationRequestService notificationRequestService;
    private final UploadProperties uploadProperties;

    @Transactional(readOnly = true)
    public ChallengeSummaryResult getChallengeSummary(Long memberId, Long groupId) {
        GroupMember currentMember = validateGroupMembership(memberId, groupId);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1);
        List<GroupMember> joinedMembers = getJoinedGroupMembers(groupId);
        List<ActivityRecord> monthlyRecords = activityRecordRepository.findActiveGroupRecordsBetween(
            groupId,
            monthStart.atStartOfDay(),
            monthEnd.atStartOfDay(),
            GroupMemberStatus.JOINED
        );

        int daysTogether = daysTogether(currentMember.getJoinedAt().toLocalDate(), today);
        int allMemberRecordedDays = allMemberRecordedDays(monthlyRecords, joinedMembers);
        int monthlyExerciseMinutes = monthlyExerciseMinutes(monthlyRecords);
        int monthlyCompletedChallengeCount = Math.toIntExact(groupChallengeRepository
            .countByGroupIdAndGroupChallengeStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanAndDeletedAtIsNull(
                groupId,
                GroupChallengeStatus.COMPLETED,
                monthStart.atStartOfDay(),
                monthEnd.atStartOfDay()
            ));
        return new ChallengeSummaryResult(
            daysTogether,
            allMemberRecordedDays,
            monthlyExerciseMinutes,
            monthlyCompletedChallengeCount
        );
    }

    @Transactional(readOnly = true)
    public NudgeTargetListResult getNudgeTargets(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        LocalDate today = LocalDate.now();
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();
        Set<Long> recordedMemberIds = activityRecordRepository.findActiveGroupRecordsBetween(
                groupId,
                startAt,
                endAt,
                GroupMemberStatus.JOINED
            )
            .stream()
            .map(ActivityRecord::getMemberId)
            .collect(Collectors.toSet());
        List<NudgeTargetResult> members = getJoinedGroupMembers(groupId)
            .stream()
            .filter(groupMember -> !groupMember.getMemberId().equals(memberId))
            .map(groupMember -> new NudgeTargetResult(
                groupMember.getMemberId(),
                groupMember.getDisplayNickname(),
                toImageUrl(groupMember.getDisplayProfileImageKey()),
                recordedMemberIds.contains(groupMember.getMemberId())
            ))
            .toList();
        return new NudgeTargetListResult(members);
    }

    @Transactional
    public void nudgeMember(Long senderMemberId, Long groupId, Long receiverMemberId) {
        if (senderMemberId.equals(receiverMemberId)) {
            throw new GeneralException(ErrorStatus.CHALLENGE_VALIDATION_FAILED);
        }
        GroupMember sender = validateGroupMembership(senderMemberId, groupId);
        validateMember(receiverMemberId);
        validateGroupMembership(receiverMemberId, groupId);

        notificationRequestService.requestBuddyNudge(
            groupId,
            sender.getMemberId(),
            sender.getDisplayNickname(),
            receiverMemberId,
            LocalDate.now().toString()
        );
    }

    private GroupMember validateGroupMembership(Long memberId, Long groupId) {
        validateMember(memberId);
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        return groupMemberRepository.findByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
                memberId,
                groupId,
                GroupMemberStatus.JOINED
            )
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
    }

    private void validateMember(Long memberId) {
        memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private List<GroupMember> getJoinedGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            groupId,
            GroupMemberStatus.JOINED
        );
    }

    private int daysTogether(LocalDate joinedOn, LocalDate today) {
        return Math.toIntExact(ChronoUnit.DAYS.between(joinedOn, today) + 1);
    }

    private int allMemberRecordedDays(List<ActivityRecord> monthlyRecords, List<GroupMember> joinedMembers) {
        Set<Long> joinedMemberIds = joinedMembers.stream()
            .map(GroupMember::getMemberId)
            .collect(Collectors.toSet());
        if (joinedMemberIds.isEmpty()) {
            return 0;
        }
        Map<LocalDate, Set<Long>> recordedMemberIdsByDate = monthlyRecords.stream()
            .collect(Collectors.groupingBy(
                record -> record.getUploadedAt().toLocalDate(),
                Collectors.mapping(ActivityRecord::getMemberId, Collectors.toSet())
            ));
        return Math.toIntExact(recordedMemberIdsByDate.values()
            .stream()
            .filter(recordedMemberIds -> recordedMemberIds.containsAll(joinedMemberIds))
            .count());
    }

    private int monthlyExerciseMinutes(List<ActivityRecord> monthlyRecords) {
        return monthlyRecords.stream()
            .filter(record -> record.getRecordType() == RecordType.EXERCISE)
            .map(ActivityRecord::getExerciseDurationMinutes)
            .filter(minutes -> minutes != null)
            .mapToInt(Integer::intValue)
            .sum();
    }

    private String toImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        String baseUrl = uploadProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + imageKey;
        }
        return baseUrl + "/" + imageKey;
    }

    public record ChallengeSummaryResult(
        int daysTogether,
        int allMemberRecordedDays,
        int monthlyExerciseMinutes,
        int monthlyCompletedChallengeCount
    ) {
    }

    public record NudgeTargetListResult(List<NudgeTargetResult> members) {
    }

    public record NudgeTargetResult(Long memberId, String nickname, String profileImageUrl, boolean recordedToday) {
    }
}
