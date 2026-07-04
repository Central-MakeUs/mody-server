package cmc.mody.notification.application;

import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationDeliveryStatus;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = "enabled", havingValue = "true")
public class NotificationDispatchScheduler {
    private final NotificationDispatchProperties dispatchProperties;
    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final String serverId;

    public NotificationDispatchScheduler(
        NotificationDispatchProperties dispatchProperties,
        NotificationRepository notificationRepository,
        NotificationSender notificationSender
    ) {
        this.dispatchProperties = dispatchProperties;
        this.notificationRepository = notificationRepository;
        this.notificationSender = notificationSender;
        this.serverId = resolveServerId();
    }

    @Scheduled(fixedDelayString = "${notification.dispatch.fixed-delay-ms:10000}")
    public void dispatchDueNotifications() {
        int batchSize = Math.max(1, dispatchProperties.getBatchSize());
        int pickedCount = notificationRepository.pickPendingNotifications(serverId, batchSize);
        if (pickedCount == 0) {
            return;
        }

        List<Notification> notifications = notificationRepository
            .findByPickedByAndDeliveryStatusAndDeletedAtIsNull(
                serverId,
                NotificationDeliveryStatus.PROCESSING,
                PageRequest.of(0, batchSize)
            );
        notifications.forEach(notification -> notificationSender.sendAsync(notification.getId()));
    }

    private String resolveServerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException exception) {
            return "unknown-" + UUID.randomUUID();
        }
    }
}
