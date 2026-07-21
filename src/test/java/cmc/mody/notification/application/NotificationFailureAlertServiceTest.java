package cmc.mody.notification.application;

import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.eq;

import cmc.mody.common.alert.ServerErrorAlertProperties;
import cmc.mody.common.alert.ServerErrorAlertSender;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationFailureAlertServiceTest {
    @Mock
    private ServerErrorAlertSender alertSender;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    void sendAlertWhenEnabled() {
        NotificationFailureAlertService service = new NotificationFailureAlertService(enabledProperties(), alertSender);
        Notification notification = notification();

        service.notifyPermanentFailure(notification, new IllegalStateException("fcm down"));

        then(alertSender).should().send(
            eq("https://hooks.slack.test/error"),
            messageCaptor.capture()
        );
        Assertions.assertThat(messageCaptor.getValue())
            .contains("Notification Dispatch Failed")
            .contains("10")
            .contains("COMMENT_CREATED")
            .contains("fcm down");
    }

    @Test
    void skipAlertWhenDisabled() {
        ServerErrorAlertProperties properties = enabledProperties();
        properties.setEnabled(false);
        NotificationFailureAlertService service = new NotificationFailureAlertService(properties, alertSender);

        service.notifyPermanentFailure(notification(), new IllegalStateException("fcm down"));

        then(alertSender).shouldHaveNoInteractions();
    }

    private ServerErrorAlertProperties enabledProperties() {
        ServerErrorAlertProperties properties = new ServerErrorAlertProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl("https://hooks.slack.test/error");
        return properties;
    }

    private Notification notification() {
        Notification notification = new Notification(
            10L,
            1L,
            NotificationType.COMMENT_CREATED,
            "댓글",
            "댓글이 달렸어요",
            null,
            "RECORD",
            100L,
            LocalDateTime.now(),
            0,
            "COMMENT_CREATED:RECORD:100:1"
        );
        notification.markProcessing("test", LocalDateTime.now());
        notification.markFailed("fcm down");
        return notification;
    }
}
