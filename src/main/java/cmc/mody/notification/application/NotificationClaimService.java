package cmc.mody.notification.application;

import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationClaimService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public List<Long> claimDueNotifications(String serverId, int batchSize) {
        List<Notification> notifications = notificationRepository.findDueNotificationsForUpdateSkipLocked(batchSize);
        LocalDateTime pickedAt = LocalDateTime.now();
        notifications.forEach(notification -> notification.markProcessing(serverId, pickedAt));
        return notifications.stream()
            .map(Notification::getId)
            .toList();
    }
}
