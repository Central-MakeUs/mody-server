package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationDeliveryStatus;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.domain.PushPlatform;
import cmc.mody.notification.infrastructure.repository.MemberPushTokenRepository;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {
    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberPushTokenRepository memberPushTokenRepository;

    @Mock
    private PushNotificationClient pushNotificationClient;

    @Mock
    private NotificationFailureAlertService notificationFailureAlertService;

    @Test
    @DisplayName("PROCESSING 알림을 FCM token으로 발송하고 SENT로 변경한다.")
    void sendSuccess() {
        NotificationSender sender = sender();
        Notification notification = processingNotification(3);
        given(notificationRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(notification));
        given(memberPushTokenRepository.findByMemberIdAndEnabledTrueAndDeletedAtIsNull(1L))
            .willReturn(List.of(pushToken()));
        given(pushNotificationClient.send(notification, List.of("fcm-token"))).willReturn(PushNotificationResult.empty());

        sender.send(10L);

        then(pushNotificationClient).should().send(eq(notification), eq(List.of("fcm-token")));
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("FCM이 invalid token을 반환하면 해당 토큰을 비활성화하고 알림은 SENT로 처리한다.")
    void sendWithInvalidTokens() {
        NotificationSender sender = sender();
        Notification notification = processingNotification(3);
        MemberPushToken pushToken = pushToken();
        given(notificationRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(notification));
        given(memberPushTokenRepository.findByMemberIdAndEnabledTrueAndDeletedAtIsNull(1L))
            .willReturn(List.of(pushToken));
        given(pushNotificationClient.send(notification, List.of("fcm-token")))
            .willReturn(new PushNotificationResult(List.of("fcm-token")));
        given(memberPushTokenRepository.findByFcmTokenInAndDeletedAtIsNull(List.of("fcm-token")))
            .willReturn(List.of(pushToken));

        sender.send(10L);

        assertThat(pushToken.isEnabled()).isFalse();
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
    }

    @Test
    @DisplayName("FCM token이 없어도 알림은 SENT로 처리한다.")
    void sendWithoutTokens() {
        NotificationSender sender = sender();
        Notification notification = processingNotification(3);
        given(notificationRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(notification));
        given(memberPushTokenRepository.findByMemberIdAndEnabledTrueAndDeletedAtIsNull(1L)).willReturn(List.of());

        sender.send(10L);

        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("발송 실패가 재시도 가능하면 PENDING으로 재예약한다.")
    void sendFailureRetry() {
        NotificationSender sender = sender();
        Notification notification = processingNotification(3);
        given(notificationRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(notification));
        given(memberPushTokenRepository.findByMemberIdAndEnabledTrueAndDeletedAtIsNull(1L))
            .willReturn(List.of(pushToken()));
        doThrow(new IllegalStateException("failed")).when(pushNotificationClient).send(any(), any());

        sender.send(10L);

        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getNextRetryAt()).isNotNull();
        assertThat(notification.getLastError()).isEqualTo("failed");
        then(notificationFailureAlertService).should(never()).notifyPermanentFailure(any(), any());
    }

    @Test
    @DisplayName("재시도 한도를 초과하면 FAILED로 변경하고 실패 알림을 요청한다.")
    void sendFailureExceeded() {
        NotificationSender sender = sender();
        Notification notification = processingNotification(0);
        IllegalStateException exception = new IllegalStateException("failed");
        given(notificationRepository.findByIdAndDeletedAtIsNull(10L)).willReturn(Optional.of(notification));
        given(memberPushTokenRepository.findByMemberIdAndEnabledTrueAndDeletedAtIsNull(1L))
            .willReturn(List.of(pushToken()));
        doThrow(exception).when(pushNotificationClient).send(any(), any());

        sender.send(10L);

        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(notification.getLastError()).isEqualTo("failed");
        then(notificationFailureAlertService).should().notifyPermanentFailure(notification, exception);
    }

    private NotificationSender sender() {
        return new NotificationSender(
            notificationRepository,
            memberPushTokenRepository,
            pushNotificationClient,
            notificationFailureAlertService
        );
    }

    private Notification processingNotification(int maxRetry) {
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
            maxRetry,
            "COMMENT_CREATED:RECORD:100:1"
        );
        notification.markProcessing("test", LocalDateTime.now());
        return notification;
    }

    private MemberPushToken pushToken() {
        return new MemberPushToken(
            20L,
            1L,
            "device-1",
            PushPlatform.IOS,
            "fcm-token",
            LocalDateTime.now()
        );
    }
}
