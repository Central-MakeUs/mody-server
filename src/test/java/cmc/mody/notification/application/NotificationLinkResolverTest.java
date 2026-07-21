package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.notification.application.event.NotificationRequestedEvent;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationLinkResolverTest {
    private final NotificationLinkResolver resolver = new NotificationLinkResolver();

    @Test
    void resolveLinkFromEvent() {
        assertThat(resolver.resolve(event(NotificationType.GROUP_MEMBER_JOINED, Map.of("groupId", 1L), "GROUP", 1L)))
            .isEqualTo("/groups/1/home");
        assertThat(resolver.resolve(event(NotificationType.EXERCISE_REMINDER, Map.of(), "MEMBER", 1L)))
            .isEqualTo("/records/exercise");
        assertThat(resolver.resolve(event(NotificationType.MEAL_REMINDER, Map.of(), "MEMBER", 1L)))
            .isEqualTo("/records/meal");
        assertThat(resolver.resolve(event(NotificationType.COMMENT_CREATED, Map.of("recordId", 10L), "RECORD", 10L)))
            .isEqualTo("/records/10");
        assertThat(resolver.resolve(event(NotificationType.GROUP_RECORD_STREAK_RISK, Map.of("groupId", 1L), "GROUP", 1L)))
            .isEqualTo("/groups/1/home");
        assertThat(resolver.resolve(event(NotificationType.BUDDY_NUDGE, Map.of("groupId", 1L), "GROUP", 1L)))
            .isEqualTo("/groups/1/home");
        assertThat(resolver.resolve(event(NotificationType.STEP_CHALLENGE_COMPLETED, Map.of("groupId", 1L), "CHALLENGE", 10L)))
            .isEqualTo("/groups/1/challenges/step");
        assertThat(resolver.resolve(
            event(NotificationType.WEEKLY_CHALLENGE_COMPLETED, Map.of("groupId", 1L), "CHALLENGE", 10L)
        )).isEqualTo("/groups/1/challenges?tab=weekly");
    }

    @Test
    void resolveFallbackLinkFromNotificationReference() {
        assertThat(resolver.resolve(notification(NotificationType.COMMENT_CREATED, "RECORD", 10L)))
            .isEqualTo("/records/10");
        assertThat(resolver.resolve(notification(NotificationType.GROUP_MEMBER_JOINED, "GROUP", 1L)))
            .isEqualTo("/groups/1/home");
    }

    private NotificationRequestedEvent event(
        NotificationType type,
        Map<String, Object> payload,
        String referenceType,
        Long referenceId
    ) {
        return NotificationRequestedEvent.immediate(type, payload, referenceType, referenceId);
    }

    private Notification notification(NotificationType type, String referenceType, Long referenceId) {
        return new Notification(
            10L,
            1L,
            type,
            "title",
            "content",
            null,
            referenceType,
            referenceId,
            LocalDateTime.now(),
            3,
            "dedupe"
        );
    }
}
