package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import cmc.mody.notification.application.event.NotificationRequestedEvent;
import cmc.mody.notification.domain.NotificationType;
import java.time.Clock;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationRequestServiceTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<NotificationRequestedEvent> eventCaptor;

    @Test
    @DisplayName("버디 기록 알림이 켜져 있으면 그룹 기준 알림 요청을 발행한다.")
    void requestBuddyRecordCreated() {
        NotificationRequestService service = service(true, "2026-10-26T12:00:00+09:00");

        service.requestBuddyRecordCreated(NotificationType.BUDDY_EXERCISE_RECORDED, 10L, 50L, 1L, "민석");

        then(eventPublisher).should().publishEvent(eventCaptor.capture());
        NotificationRequestedEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(NotificationType.BUDDY_EXERCISE_RECORDED);
        assertThat(event.referenceType()).isEqualTo("RECORD");
        assertThat(event.referenceId()).isEqualTo(50L);
        assertThat(event.payload())
            .containsEntry("groupId", 10L)
            .containsEntry("recordId", 50L)
            .containsEntry("writerMemberId", 1L)
            .containsEntry("nickname", "민석");
    }

    @Test
    @DisplayName("버디 기록 알림이 꺼져 있으면 알림 요청을 발행하지 않는다.")
    void skipWhenDisabled() {
        NotificationRequestService service = service(false, "2026-10-26T12:00:00+09:00");

        service.requestBuddyRecordCreated(NotificationType.BUDDY_MEAL_RECORDED, 10L, 50L, 1L, "민석");

        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("새벽 0시부터 5시 전까지 올라온 기록은 버디 기록 알림을 만들지 않는다.")
    void skipDuringQuietTime() {
        NotificationRequestService atMidnight = service(true, "2026-10-26T00:00:00+09:00");
        NotificationRequestService beforeFive = service(true, "2026-10-26T04:59:59+09:00");

        atMidnight.requestBuddyRecordCreated(NotificationType.BUDDY_MEAL_RECORDED, 10L, 50L, 1L, "민석");
        beforeFive.requestBuddyRecordCreated(NotificationType.BUDDY_MEAL_RECORDED, 10L, 51L, 1L, "민석");

        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("방해 금지 시간이 자정을 넘겨도 구간을 판단한다.")
    void quietTimeAcrossMidnight() {
        NotificationBuddyRecordProperties properties = new NotificationBuddyRecordProperties();
        properties.setQuietStart(LocalTime.of(23, 0));
        properties.setQuietEnd(LocalTime.of(5, 0));

        assertThat(properties.isQuietTime(LocalTime.of(23, 30))).isTrue();
        assertThat(properties.isQuietTime(LocalTime.of(4, 59))).isTrue();
        assertThat(properties.isQuietTime(LocalTime.of(5, 0))).isFalse();
        assertThat(properties.isQuietTime(LocalTime.of(22, 59))).isFalse();
    }

    private NotificationRequestService service(boolean enabled, String now) {
        NotificationBuddyRecordProperties properties = new NotificationBuddyRecordProperties();
        properties.setEnabled(enabled);
        NotificationRequestService service = new NotificationRequestService(eventPublisher, properties);
        service.useClock(Clock.fixed(OffsetDateTime.parse(now).toInstant(), KST));
        return service;
    }
}
