package cmc.mody.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import cmc.mody.common.id.IdGenerator;
import cmc.mody.notification.application.NotificationDispatchProperties;
import cmc.mody.notification.application.NotificationRecipientResolver;
import cmc.mody.notification.application.NotificationTemplate;
import cmc.mody.notification.application.NotificationTemplateRenderer;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationDeliveryStatus;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.infrastructure.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRequestedEventListenerTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private NotificationRecipientResolver recipientResolver;

    @Mock
    private NotificationTemplateRenderer templateRenderer;

    @Mock
    private NotificationRepository notificationRepository;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Test
    @DisplayName("알림 요청 이벤트를 notification outbox row로 저장한다.")
    void createNotifications() {
        NotificationRequestedEventListener listener = listener();
        NotificationRequestedEvent event = commentEvent();
        given(recipientResolver.resolve(event)).willReturn(List.of(1L));
        given(templateRenderer.render(NotificationType.COMMENT_CREATED, event.payload()))
            .willReturn(new NotificationTemplate("댓글", "댓글이 달렸어요"));
        given(notificationRepository.existsByDedupeKeyAndDeletedAtIsNull("COMMENT_CREATED:RECORD:10:1"))
            .willReturn(false);
        given(idGenerator.nextId()).willReturn(100L);

        listener.createNotifications(event);

        then(notificationRepository).should().save(notificationCaptor.capture());
        Notification notification = notificationCaptor.getValue();
        assertThat(notification.getId()).isEqualTo(100L);
        assertThat(notification.getReceiverMemberId()).isEqualTo(1L);
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.COMMENT_CREATED);
        assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(notification.getDedupeKey()).isEqualTo("COMMENT_CREATED:RECORD:10:1");
    }

    @Test
    @DisplayName("dedupe key가 이미 있으면 중복 알림을 저장하지 않는다.")
    void skipDuplicatedNotification() {
        NotificationRequestedEventListener listener = listener();
        NotificationRequestedEvent event = commentEvent();
        given(recipientResolver.resolve(event)).willReturn(List.of(1L));
        given(templateRenderer.render(NotificationType.COMMENT_CREATED, event.payload()))
            .willReturn(new NotificationTemplate("댓글", "댓글이 달렸어요"));
        given(notificationRepository.existsByDedupeKeyAndDeletedAtIsNull("COMMENT_CREATED:RECORD:10:1"))
            .willReturn(true);

        listener.createNotifications(event);

        then(notificationRepository).should(never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("그룹 연속 기록 위험 알림은 그룹과 날짜를 포함한 dedupe key로 저장한다.")
    void createGroupRecordStreakRiskNotification() {
        NotificationRequestedEventListener listener = listener();
        NotificationRequestedEvent event = NotificationRequestedEvent.immediate(
            NotificationType.GROUP_RECORD_STREAK_RISK,
            Map.of(
                "groupId", 10L,
                "receiverMemberId", 1L,
                "nickname", "민석",
                "date", "2026-07-04"
            ),
            "GROUP",
            10L
        );
        given(recipientResolver.resolve(event)).willReturn(List.of(1L));
        given(templateRenderer.render(NotificationType.GROUP_RECORD_STREAK_RISK, event.payload()))
            .willReturn(new NotificationTemplate("민석님 어디가셨나요 ㅠㅠ", "오늘 기록하지 않으면 그룹 연속 기록이 깨져요!"));
        given(notificationRepository.existsByDedupeKeyAndDeletedAtIsNull(
            "GROUP_RECORD_STREAK_RISK:DATE:10:1:2026-07-04"
        )).willReturn(false);
        given(idGenerator.nextId()).willReturn(101L);

        listener.createNotifications(event);

        then(notificationRepository).should().save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getDedupeKey())
            .isEqualTo("GROUP_RECORD_STREAK_RISK:DATE:10:1:2026-07-04");
    }

    private NotificationRequestedEventListener listener() {
        NotificationDispatchProperties properties = new NotificationDispatchProperties();
        properties.setMaxRetry(3);
        return new NotificationRequestedEventListener(
            idGenerator,
            properties,
            recipientResolver,
            templateRenderer,
            notificationRepository
        );
    }

    private NotificationRequestedEvent commentEvent() {
        return NotificationRequestedEvent.immediate(
            NotificationType.COMMENT_CREATED,
            Map.of(
                "recordId", 10L,
                "commenterMemberId", 2L
            ),
            "RECORD",
            10L
        );
    }
}
