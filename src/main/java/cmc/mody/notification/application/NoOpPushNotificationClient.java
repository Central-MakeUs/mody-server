package cmc.mody.notification.application;

import cmc.mody.notification.domain.Notification;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushNotificationClient implements PushNotificationClient {
    @Override
    public PushNotificationResult send(Notification notification, List<String> tokens) {
        log.debug("Skip push notification. notificationId={}, tokenCount={}", notification.getId(), tokens.size());
        return PushNotificationResult.empty();
    }
}
