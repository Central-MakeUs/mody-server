package cmc.mody.notification.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.notification.application.event.NotificationRequestedEvent;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationRecipientResolver {
    private final ActivityRecordRepository activityRecordRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    public NotificationRecipientResolver(
        ActivityRecordRepository activityRecordRepository,
        GroupMemberRepository groupMemberRepository,
        NotificationSettingRepository notificationSettingRepository
    ) {
        this.activityRecordRepository = activityRecordRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.notificationSettingRepository = notificationSettingRepository;
    }

    public List<Long> resolve(NotificationRequestedEvent event) {
        Map<String, Object> payload = event.payload();
        return switch (event.type()) {
            case GROUP_MEMBER_JOINED -> resolveGroupMemberJoined(payload);
            case EXERCISE_REMINDER, MEAL_REMINDER, GROUP_RECORD_STREAK_RISK, BUDDY_NUDGE ->
                List.of(NotificationPayload.requireLong(payload, "receiverMemberId"));
            case COMMENT_CREATED -> resolveComment(payload);
            case STEP_CHALLENGE_COMPLETED, WEEKLY_CHALLENGE_COMPLETED -> resolveGroupMembers(payload);
            default -> throw new GeneralException(ErrorStatus.NOTIFICATION_UNSUPPORTED_TYPE);
        };
    }

    private List<Long> resolveGroupMemberJoined(Map<String, Object> payload) {
        Long groupId = NotificationPayload.requireLong(payload, "groupId");
        Long joinedMemberId = NotificationPayload.requireLong(payload, "joinedMemberId");
        return groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .map(GroupMember::getMemberId)
            .filter(memberId -> !memberId.equals(joinedMemberId))
            .toList();
    }

    private List<Long> resolveComment(Map<String, Object> payload) {
        Long recordId = NotificationPayload.requireLong(payload, "recordId");
        Long commenterMemberId = NotificationPayload.requireLong(payload, "commenterMemberId");
        ActivityRecord record = activityRecordRepository.findById(recordId)
            .filter(ActivityRecord::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        Long receiverMemberId = record.getMemberId();
        if (receiverMemberId.equals(commenterMemberId) || !isCommentNotificationEnabled(receiverMemberId)) {
            return List.of();
        }
        return List.of(receiverMemberId);
    }

    private List<Long> resolveGroupMembers(Map<String, Object> payload) {
        Long groupId = NotificationPayload.requireLong(payload, "groupId");
        return groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .map(GroupMember::getMemberId)
            .toList();
    }

    private boolean isCommentNotificationEnabled(Long memberId) {
        return notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(memberId)
            .map(NotificationSetting::isCommentNotificationEnabled)
            .orElse(true);
    }
}
