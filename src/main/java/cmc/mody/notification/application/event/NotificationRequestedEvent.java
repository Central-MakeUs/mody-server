package cmc.mody.notification.application.event;

import cmc.mody.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.Map;

public record NotificationRequestedEvent(
    NotificationType type,
    Map<String, Object> payload,
    LocalDateTime scheduledAt,
    Long referenceId,
    String referenceType,
    String imageKey
) {
    public static NotificationRequestedEvent immediate(
        NotificationType type,
        Map<String, Object> payload,
        String referenceType,
        Long referenceId
    ) {
        return new NotificationRequestedEvent(type, payload, null, referenceId, referenceType, null);
    }

    public static NotificationRequestedEvent scheduled(
        NotificationType type,
        LocalDateTime scheduledAt,
        Map<String, Object> payload,
        String referenceType,
        Long referenceId
    ) {
        return new NotificationRequestedEvent(type, payload, scheduledAt, referenceId, referenceType, null);
    }
}
