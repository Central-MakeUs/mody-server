package cmc.mody.notification.application;

import cmc.mody.notification.application.event.NotificationRequestedEvent;
import cmc.mody.notification.domain.Notification;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NotificationLinkResolver {
    public String resolve(NotificationRequestedEvent event) {
        Map<String, Object> payload = event.payload();
        return switch (event.type()) {
            case GROUP_MEMBER_JOINED -> groupHome(NotificationPayload.requireLong(payload, "groupId"));
            case EXERCISE_REMINDER -> "/records/exercise";
            case MEAL_REMINDER -> "/records/meal";
            case COMMENT_CREATED -> recordDetail(NotificationPayload.requireLong(payload, "recordId"));
            case GROUP_RECORD_STREAK_RISK -> groupHome(NotificationPayload.requireLong(payload, "groupId"));
            case BUDDY_NUDGE -> groupHome(NotificationPayload.requireLong(payload, "groupId"));
            case STEP_CHALLENGE_COMPLETED -> stepChallenge(groupIdFromPayloadOrReference(payload, event));
            case WEEKLY_CHALLENGE_COMPLETED -> weeklyChallenge(groupIdFromPayloadOrReference(payload, event));
            default -> null;
        };
    }

    public String resolve(Notification notification) {
        if (StringUtils.hasText(notification.getLink())) {
            return notification.getLink();
        }

        Long referenceId = notification.getReferenceId();
        return switch (notification.getNotificationType()) {
            case GROUP_MEMBER_JOINED, GROUP_RECORD_STREAK_RISK, BUDDY_NUDGE -> groupHome(referenceId);
            case EXERCISE_REMINDER -> "/records/exercise";
            case MEAL_REMINDER -> "/records/meal";
            case COMMENT_CREATED -> recordDetail(referenceId);
            case STEP_CHALLENGE_COMPLETED -> stepChallenge(groupReferenceId(notification));
            case WEEKLY_CHALLENGE_COMPLETED -> weeklyChallenge(groupReferenceId(notification));
            default -> null;
        };
    }

    private Long groupIdFromPayloadOrReference(Map<String, Object> payload, NotificationRequestedEvent event) {
        Object groupId = payload.get("groupId");
        if (groupId instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if ("GROUP".equals(event.referenceType())) {
            return event.referenceId();
        }
        return null;
    }

    private Long groupReferenceId(Notification notification) {
        if ("GROUP".equals(notification.getReferenceType())) {
            return notification.getReferenceId();
        }
        return null;
    }

    private String groupHome(Long groupId) {
        return groupId == null ? "/home" : "/groups/%d/home".formatted(groupId);
    }

    private String recordDetail(Long recordId) {
        return recordId == null ? "/records" : "/records/%d".formatted(recordId);
    }

    private String stepChallenge(Long groupId) {
        return groupId == null ? "/challenges/step" : "/groups/%d/challenges/step".formatted(groupId);
    }

    private String weeklyChallenge(Long groupId) {
        return groupId == null ? "/challenges?tab=weekly" : "/groups/%d/challenges?tab=weekly".formatted(groupId);
    }
}
