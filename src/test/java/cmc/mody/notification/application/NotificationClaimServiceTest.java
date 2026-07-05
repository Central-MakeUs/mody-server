package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationDeliveryStatus;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationClaimServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("SKIP LOCKED로 조회한 due 알림을 PROCESSING으로 선점한다.")
    void claimDueNotifications() {
        NotificationClaimService service = new NotificationClaimService(notificationRepository);
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
            3,
            "COMMENT_CREATED:RECORD:100:1"
        );
        given(notificationRepository.findDueNotificationsForUpdateSkipLocked(20))
            .willReturn(List.of(notification));

        List<Long> notificationIds = service.claimDueNotifications("server-1", 20);

        assertThat(notificationIds).containsExactly(10L);
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PROCESSING);
        assertThat(notification.getPickedBy()).isEqualTo("server-1");
        assertThat(notification.getPickedAt()).isNotNull();
        then(notificationRepository).should().findDueNotificationsForUpdateSkipLocked(20);
    }
}
