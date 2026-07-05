package cmc.mody.notification.application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification.dispatch", name = "enabled", havingValue = "true")
public class NotificationDispatchScheduler {
    private final NotificationDispatchProperties dispatchProperties;
    private final NotificationClaimService notificationClaimService;
    private final NotificationSender notificationSender;
    private final String serverId;

    public NotificationDispatchScheduler(
        NotificationDispatchProperties dispatchProperties,
        NotificationClaimService notificationClaimService,
        NotificationSender notificationSender
    ) {
        this.dispatchProperties = dispatchProperties;
        this.notificationClaimService = notificationClaimService;
        this.notificationSender = notificationSender;
        this.serverId = resolveServerId();
    }

    @Scheduled(fixedDelayString = "${notification.dispatch.fixed-delay-ms:10000}")
    public void dispatchDueNotifications() {
        int batchSize = Math.max(1, dispatchProperties.getBatchSize());
        List<Long> notificationIds = notificationClaimService.claimDueNotifications(serverId, batchSize);
        if (notificationIds.isEmpty()) {
            return;
        }
        notificationIds.forEach(notificationSender::sendAsync);
    }

    private String resolveServerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException exception) {
            return "unknown-" + UUID.randomUUID();
        }
    }
}
