package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationRetentionSchedulerTest {
    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("보관 기간이 지난 알림을 배치 크기만큼 조회해서 소프트 삭제한다.")
    void deleteExpiredNotifications() {
        NotificationRetentionProperties properties = new NotificationRetentionProperties();
        properties.setBatchSize(2);
        NotificationRetentionScheduler scheduler = new NotificationRetentionScheduler(properties, notificationRepository);
        Notification notification = new Notification(
            10L,
            1L,
            NotificationType.COMMENT_CREATED,
            "댓글",
            "댓글이 달렸어요"
        );
        LocalDateTime threshold = LocalDateTime.of(2026, 7, 4, 4, 0);
        given(notificationRepository.findByCreatedAtBeforeAndDeletedAtIsNull(any(), any()))
            .willReturn(List.of(notification));

        scheduler.deleteExpiredNotifications(threshold);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByCreatedAtBeforeAndDeletedAtIsNull(
            org.mockito.ArgumentMatchers.eq(threshold),
            pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
        assertThat(notification.getDeletedAt()).isNotNull();
    }
}
