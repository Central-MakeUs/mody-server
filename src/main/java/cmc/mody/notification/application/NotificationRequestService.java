package cmc.mody.notification.application;

import cmc.mody.notification.application.event.NotificationRequestedEvent;
import cmc.mody.notification.domain.NotificationType;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationRequestService {
    private final ApplicationEventPublisher eventPublisher;

    public NotificationRequestService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void request(NotificationRequestedEvent event) {
        eventPublisher.publishEvent(event);
    }

    public void requestCommentCreated(Long recordId, Long commenterMemberId) {
        request(NotificationRequestedEvent.immediate(
            NotificationType.COMMENT_CREATED,
            Map.of(
                "recordId", recordId,
                "commenterMemberId", commenterMemberId
            ),
            "RECORD",
            recordId
        ));
    }

    public void requestGroupMemberJoined(Long groupId, String groupName, Long joinedMemberId, String nickname) {
        request(NotificationRequestedEvent.immediate(
            NotificationType.GROUP_MEMBER_JOINED,
            Map.of(
                "groupId", groupId,
                "groupName", groupName,
                "joinedMemberId", joinedMemberId,
                "nickname", nickname
            ),
            "GROUP",
            groupId
        ));
    }

    public void requestMealReminder(Long memberId, String mealType, String date) {
        request(NotificationRequestedEvent.immediate(
            NotificationType.MEAL_REMINDER,
            Map.of(
                "receiverMemberId", memberId,
                "mealType", mealType,
                "date", date
            ),
            "MEMBER",
            memberId
        ));
    }

    public void requestExerciseReminder(Long memberId, String scheduledTime, String date) {
        request(NotificationRequestedEvent.immediate(
            NotificationType.EXERCISE_REMINDER,
            Map.of(
                "receiverMemberId", memberId,
                "scheduledTime", scheduledTime,
                "date", date
            ),
            "MEMBER",
            memberId
        ));
    }

    public void requestGroupRecordStreakRisk(Long groupId, Long memberId, String nickname, String date) {
        request(NotificationRequestedEvent.immediate(
            NotificationType.GROUP_RECORD_STREAK_RISK,
            Map.of(
                "groupId", groupId,
                "receiverMemberId", memberId,
                "nickname", nickname,
                "date", date
            ),
            "GROUP",
            groupId
        ));
    }

    public void requestBuddyNudge(
        Long groupId,
        Long senderMemberId,
        String senderNickname,
        Long receiverMemberId,
        String date
    ) {
        request(NotificationRequestedEvent.immediate(
            NotificationType.BUDDY_NUDGE,
            Map.of(
                "groupId", groupId,
                "senderMemberId", senderMemberId,
                "nickname", senderNickname,
                "receiverMemberId", receiverMemberId,
                "date", date
            ),
            "GROUP",
            groupId
        ));
    }

    public void requestWeeklyChallengeCompleted(Long groupId, Long groupChallengeId) {
        request(NotificationRequestedEvent.immediate(
            NotificationType.WEEKLY_CHALLENGE_COMPLETED,
            Map.of("groupId", groupId),
            "CHALLENGE",
            groupChallengeId
        ));
    }
}
