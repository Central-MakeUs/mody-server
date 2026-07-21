package cmc.mody.notification.application.event;

import cmc.mody.common.id.IdGenerator;
import cmc.mody.notification.application.NotificationDispatchProperties;
import cmc.mody.notification.application.NotificationLinkResolver;
import cmc.mody.notification.application.NotificationPayload;
import cmc.mody.notification.application.NotificationRecipientResolver;
import cmc.mody.notification.application.NotificationTemplate;
import cmc.mody.notification.application.NotificationTemplateRenderer;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationRequestedEventListener {
    private final IdGenerator idGenerator;
    private final NotificationDispatchProperties dispatchProperties;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationTemplateRenderer templateRenderer;
    private final NotificationLinkResolver linkResolver;
    private final NotificationRepository notificationRepository;

    public NotificationRequestedEventListener(
        IdGenerator idGenerator,
        NotificationDispatchProperties dispatchProperties,
        NotificationRecipientResolver recipientResolver,
        NotificationTemplateRenderer templateRenderer,
        NotificationLinkResolver linkResolver,
        NotificationRepository notificationRepository
    ) {
        this.idGenerator = idGenerator;
        this.dispatchProperties = dispatchProperties;
        this.recipientResolver = recipientResolver;
        this.templateRenderer = templateRenderer;
        this.linkResolver = linkResolver;
        this.notificationRepository = notificationRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleTransactional(NotificationRequestedEvent event) {
        createNotifications(event);
    }

    void createNotifications(NotificationRequestedEvent event) {
        List<Long> receiverMemberIds = recipientResolver.resolve(event);
        if (receiverMemberIds.isEmpty()) {
            return;
        }

        NotificationTemplate template = templateRenderer.render(event.type(), event.payload());
        LocalDateTime scheduledAt = event.scheduledAt() == null ? LocalDateTime.now() : event.scheduledAt();
        for (Long receiverMemberId : receiverMemberIds) {
            saveIfNotDuplicated(event, receiverMemberId, template, scheduledAt);
        }
    }

    private void saveIfNotDuplicated(
        NotificationRequestedEvent event,
        Long receiverMemberId,
        NotificationTemplate template,
        LocalDateTime scheduledAt
    ) {
        String dedupeKey = createDedupeKey(event, receiverMemberId);
        if (notificationRepository.existsByDedupeKeyAndDeletedAtIsNull(dedupeKey)) {
            return;
        }

        try {
            notificationRepository.save(new Notification(
                idGenerator.nextId(),
                receiverMemberId,
                event.type(),
                template.title(),
                template.content(),
                event.imageKey(),
                event.referenceType(),
                event.referenceId(),
                linkResolver.resolve(event),
                scheduledAt,
                dispatchProperties.getMaxRetry(),
                dedupeKey
            ));
        } catch (DataIntegrityViolationException ignored) {
            // 다른 서버나 스레드가 먼저 저장한 경우 중복 알림으로 보고 무시한다.
        }
    }

    private String createDedupeKey(NotificationRequestedEvent event, Long receiverMemberId) {
        Map<String, Object> payload = event.payload();
        LocalDate today = LocalDate.now();
        return switch (event.type()) {
            case GROUP_MEMBER_JOINED -> String.join(":",
                event.type().name(),
                "GROUP",
                NotificationPayload.requireLong(payload, "groupId").toString(),
                NotificationPayload.requireLong(payload, "joinedMemberId").toString(),
                receiverMemberId.toString()
            );
            case COMMENT_CREATED -> String.join(":",
                event.type().name(),
                "RECORD",
                NotificationPayload.requireLong(payload, "recordId").toString(),
                receiverMemberId.toString()
            );
            case MEAL_REMINDER -> String.join(":",
                event.type().name(),
                "DATE",
                receiverMemberId.toString(),
                String.valueOf(payload.getOrDefault("mealType", "UNKNOWN")),
                String.valueOf(payload.getOrDefault("date", today.toString()))
            );
            case EXERCISE_REMINDER -> String.join(":",
                event.type().name(),
                "DATE",
                receiverMemberId.toString(),
                String.valueOf(payload.getOrDefault("scheduledTime", "UNKNOWN")),
                String.valueOf(payload.getOrDefault("date", today.toString()))
            );
            case GROUP_RECORD_STREAK_RISK -> String.join(":",
                event.type().name(),
                "DATE",
                NotificationPayload.requireLong(payload, "groupId").toString(),
                receiverMemberId.toString(),
                String.valueOf(payload.getOrDefault("date", today.toString()))
            );
            case BUDDY_NUDGE -> String.join(":",
                event.type().name(),
                "DATE",
                NotificationPayload.requireLong(payload, "senderMemberId").toString(),
                receiverMemberId.toString(),
                String.valueOf(payload.getOrDefault("date", today.toString()))
            );
            case STEP_CHALLENGE_COMPLETED, WEEKLY_CHALLENGE_COMPLETED -> String.join(":",
                event.type().name(),
                "CHALLENGE",
                String.valueOf(event.referenceId()),
                receiverMemberId.toString()
            );
            default -> String.join(":",
                event.type().name(),
                String.valueOf(event.referenceType()),
                String.valueOf(event.referenceId()),
                receiverMemberId.toString()
            );
        };
    }
}
