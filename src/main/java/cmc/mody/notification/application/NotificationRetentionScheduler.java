package cmc.mody.notification.application;

import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "notification.retention", name = "enabled", havingValue = "true")
public class NotificationRetentionScheduler {
    private final NotificationRetentionProperties retentionProperties;
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "${notification.retention.cron:0 0 4 * * *}")
    @Transactional
    public void deleteExpiredNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionProperties.getRetentionDays());
        deleteExpiredNotifications(threshold);
    }

    void deleteExpiredNotifications(LocalDateTime threshold) {
        List<Notification> notifications = notificationRepository.findByCreatedAtBeforeAndDeletedAtIsNull(
            threshold,
            PageRequest.of(0, retentionProperties.getBatchSize())
        );
        notifications.forEach(Notification::delete);
    }
}
