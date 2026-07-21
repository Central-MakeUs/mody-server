package cmc.mody.notification.application;

import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationDeliveryStatus;
import cmc.mody.notification.infrastructure.repository.MemberPushTokenRepository;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NotificationSender {
    private final NotificationRepository notificationRepository;
    private final MemberPushTokenRepository memberPushTokenRepository;
    private final PushNotificationClient pushNotificationClient;
    private final NotificationFailureAlertService notificationFailureAlertService;

    public NotificationSender(
        NotificationRepository notificationRepository,
        MemberPushTokenRepository memberPushTokenRepository,
        PushNotificationClient pushNotificationClient,
        NotificationFailureAlertService notificationFailureAlertService
    ) {
        this.notificationRepository = notificationRepository;
        this.memberPushTokenRepository = memberPushTokenRepository;
        this.pushNotificationClient = pushNotificationClient;
        this.notificationFailureAlertService = notificationFailureAlertService;
    }

    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendAsync(Long notificationId) {
        send(notificationId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Long notificationId) {
        Optional<Notification> optionalNotification = notificationRepository.findByIdAndDeletedAtIsNull(notificationId);
        if (optionalNotification.isEmpty()) {
            return;
        }

        Notification notification = optionalNotification.get();
        if (notification.getDeliveryStatus() != NotificationDeliveryStatus.PROCESSING) {
            return;
        }

        try {
            List<String> tokens = memberPushTokenRepository
                .findByMemberIdAndEnabledTrueAndDeletedAtIsNull(notification.getReceiverMemberId())
                .stream()
                .map(MemberPushToken::getFcmToken)
                .toList();
            if (!tokens.isEmpty()) {
                PushNotificationResult result = pushNotificationClient.send(notification, tokens);
                disableInvalidTokens(result);
            }
            notification.markSent(LocalDateTime.now());
        } catch (RuntimeException exception) {
            handleFailure(notification, exception);
        }
    }

    private void disableInvalidTokens(PushNotificationResult result) {
        if (!result.hasInvalidTokens()) {
            return;
        }
        memberPushTokenRepository.findByFcmTokenInAndDeletedAtIsNull(result.invalidTokens())
            .forEach(MemberPushToken::disable);
    }

    private void handleFailure(Notification notification, RuntimeException exception) {
        if (notification.canRetry()) {
            notification.reschedule(nextRetryAt(notification), exception.getMessage());
            log.warn(
                "Notification push failed. notificationId={}, retryCount={}, nextRetryAt={}",
                notification.getId(),
                notification.getRetryCount(),
                notification.getNextRetryAt(),
                exception
            );
            return;
        }

        notification.markFailed(exception.getMessage());
        notificationFailureAlertService.notifyPermanentFailure(notification, exception);
        log.warn("Notification push failed permanently. notificationId={}", notification.getId(), exception);
    }

    private LocalDateTime nextRetryAt(Notification notification) {
        int nextRetryCount = notification.getRetryCount() + 1;
        long minutes = switch (nextRetryCount) {
            case 1 -> 1L;
            case 2 -> 5L;
            default -> 30L;
        };
        return LocalDateTime.now().plusMinutes(minutes);
    }
}
