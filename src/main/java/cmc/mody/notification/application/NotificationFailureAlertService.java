package cmc.mody.notification.application;

import cmc.mody.common.alert.ServerErrorAlertProperties;
import cmc.mody.common.alert.ServerErrorAlertSender;
import cmc.mody.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFailureAlertService {
    private static final String UNKNOWN = "unknown";

    private final ServerErrorAlertProperties properties;
    private final ServerErrorAlertSender alertSender;

    public void notifyPermanentFailure(Notification notification, Throwable exception) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getWebhookUrl())) {
            return;
        }

        try {
            alertSender.send(properties.getWebhookUrl(), format(notification, exception));
        } catch (Exception alertException) {
            log.warn("Failed to build notification failure alert: {}", alertException.getMessage());
        }
    }

    private String format(Notification notification, Throwable exception) {
        String text = """
            :warning: *Notification Dispatch Failed*
            *Notification*: `%s`
            *Receiver*: `%s`
            *Type*: `%s`
            *Reference*: `%s / %s`
            *Retry*: `%d / %d`
            *Delivery Status*: `%s`
            *Exception*: `%s`
            *Message*: `%s`
            """.formatted(
            notification.getId(),
            notification.getReceiverMemberId(),
            notification.getNotificationType(),
            valueOrUnknown(notification.getReferenceType()),
            notification.getReferenceId() == null ? UNKNOWN : notification.getReferenceId(),
            notification.getRetryCount(),
            notification.getMaxRetry(),
            notification.getDeliveryStatus(),
            exception.getClass().getName(),
            valueOrUnknown(exception.getMessage())
        );
        return truncate(text);
    }

    private String truncate(String text) {
        int maxLength = Math.max(500, properties.getMaxMessageLength());
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 15) + "\n...(truncated)";
    }

    private String valueOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN;
    }
}
