package cmc.mody.notification.application;

import cmc.mody.notification.domain.Notification;
import java.util.List;

public interface PushNotificationClient {
    PushNotificationResult send(Notification notification, List<String> tokens);
}
