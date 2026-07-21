package cmc.mody.notification.infrastructure.fcm;

import cmc.mody.notification.application.NotificationLinkResolver;
import cmc.mody.notification.application.PushNotificationClient;
import cmc.mody.notification.application.PushNotificationResult;
import cmc.mody.notification.domain.Notification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "fcm", name = "enabled", havingValue = "true")
public class FcmPushNotificationClient implements PushNotificationClient {
    private static final int FCM_MULTICAST_LIMIT = 500;

    @SuppressWarnings("unused")
    private final FirebaseInitializer firebaseInitializer;
    private final NotificationLinkResolver linkResolver;

    public FcmPushNotificationClient(FirebaseInitializer firebaseInitializer, NotificationLinkResolver linkResolver) {
        this.firebaseInitializer = firebaseInitializer;
        this.linkResolver = linkResolver;
    }

    @Override
    public PushNotificationResult send(Notification notification, List<String> tokens) {
        List<String> invalidTokens = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += FCM_MULTICAST_LIMIT) {
            int end = Math.min(start + FCM_MULTICAST_LIMIT, tokens.size());
            invalidTokens.addAll(sendChunk(notification, tokens.subList(start, end)));
        }
        return new PushNotificationResult(invalidTokens);
    }

    private List<String> sendChunk(Notification notification, List<String> tokens) {
        MulticastMessage message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(com.google.firebase.messaging.Notification.builder()
                .setTitle(notification.getTitle())
                .setBody(notification.getContent())
                .build())
            .putAllData(data(notification))
            .build();

        try {
            List<SendResponse> responses = FirebaseMessaging.getInstance()
                .sendEachForMulticast(message)
                .getResponses();
            List<String> invalidTokens = new ArrayList<>();
            for (int index = 0; index < responses.size(); index++) {
                SendResponse response = responses.get(index);
                if (!response.isSuccessful()) {
                    FirebaseMessagingException exception = response.getException();
                    if (isInvalidToken(exception)) {
                        invalidTokens.add(tokens.get(index));
                    }
                    log.warn(
                        "FCM token send failed. notificationId={}, tokenIndex={}, error={}",
                        notification.getId(),
                        index,
                        exception == null ? null : exception.getMessage()
                    );
                }
            }
            return invalidTokens;
        } catch (FirebaseMessagingException exception) {
            throw new IllegalStateException("FCM 발송에 실패했습니다.", exception);
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        if (exception == null) {
            return false;
        }
        MessagingErrorCode errorCode = exception.getMessagingErrorCode();
        return errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private Map<String, String> data(Notification notification) {
        Map<String, String> data = new LinkedHashMap<>();
        put(data, "notificationId", notification.getId());
        put(data, "type", notification.getNotificationType());
        put(data, "referenceType", notification.getReferenceType());
        put(data, "referenceId", notification.getReferenceId());
        put(data, "link", linkResolver.resolve(notification));
        put(data, "imageKey", notification.getImageKey());
        return data;
    }

    private void put(Map<String, String> data, String key, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value);
        if (StringUtils.hasText(text)) {
            data.put(key, text);
        }
    }
}
