package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import cmc.mody.common.alert.ServerErrorAlertProperties;
import cmc.mody.common.alert.ServerErrorAlertSender;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.PushPlatform;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushTokenInvalidationAlertServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

    @Mock
    private ServerErrorAlertSender alertSender;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    @Test
    @DisplayName("비활성화된 토큰이 기준치 미만이면 알림을 보내지 않는다.")
    void skipBelowThreshold() {
        PushTokenInvalidationAlertService service = service(alertProperties());
        service.useClock(Clock.fixed(NOW, ZoneOffset.UTC));

        service.record(tokens(PushPlatform.IOS, 2));

        then(alertSender).should(never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("집계 시간 안에 기준치를 넘으면 플랫폼별 개수와 함께 알림을 보낸다.")
    void sendWhenThresholdReached() {
        PushTokenInvalidationAlertService service = service(alertProperties());
        service.useClock(Clock.fixed(NOW, ZoneOffset.UTC));

        service.record(tokens(PushPlatform.IOS, 2));
        service.useClock(Clock.fixed(NOW.plus(Duration.ofMinutes(5)), ZoneOffset.UTC));
        service.record(tokens(PushPlatform.ANDROID, 1));

        then(alertSender).should().send(eq("https://hooks.slack.test/error"), messageCaptor.capture());
        assertThat(messageCaptor.getValue())
            .contains("Push Tokens Disabled In Bulk")
            .contains("`3` in last `10` min")
            .contains("IOS 2 / ANDROID 1");
    }

    @Test
    @DisplayName("집계 시간이 지난 토큰은 개수에서 제외한다.")
    void evictExpiredTokens() {
        PushTokenInvalidationAlertService service = service(alertProperties());
        service.useClock(Clock.fixed(NOW, ZoneOffset.UTC));

        service.record(tokens(PushPlatform.IOS, 2));
        service.useClock(Clock.fixed(NOW.plus(Duration.ofMinutes(11)), ZoneOffset.UTC));
        service.record(tokens(PushPlatform.IOS, 1));

        then(alertSender).should(never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("쿨다운 동안에는 기준치를 넘어도 다시 알리지 않는다.")
    void suppressDuringCooldown() {
        PushTokenInvalidationAlertService service = service(alertProperties());
        service.useClock(Clock.fixed(NOW, ZoneOffset.UTC));
        service.record(tokens(PushPlatform.IOS, 3));

        service.useClock(Clock.fixed(NOW.plus(Duration.ofMinutes(30)), ZoneOffset.UTC));
        service.record(tokens(PushPlatform.IOS, 3));
        service.useClock(Clock.fixed(NOW.plus(Duration.ofMinutes(61)), ZoneOffset.UTC));
        service.record(tokens(PushPlatform.IOS, 3));

        then(alertSender).should(times(2)).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Slack 알림 설정이 꺼져 있으면 아무것도 하지 않는다.")
    void skipWhenAlertDisabled() {
        ServerErrorAlertProperties properties = alertProperties();
        properties.setEnabled(false);
        PushTokenInvalidationAlertService service = service(properties);

        service.record(tokens(PushPlatform.IOS, 5));

        then(alertSender).shouldHaveNoInteractions();
    }

    private PushTokenInvalidationAlertService service(ServerErrorAlertProperties alertProperties) {
        NotificationTokenAlertProperties tokenAlertProperties = new NotificationTokenAlertProperties();
        tokenAlertProperties.setThreshold(3);
        return new PushTokenInvalidationAlertService(tokenAlertProperties, alertProperties, alertSender);
    }

    private ServerErrorAlertProperties alertProperties() {
        ServerErrorAlertProperties properties = new ServerErrorAlertProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl("https://hooks.slack.test/error");
        return properties;
    }

    private List<MemberPushToken> tokens(PushPlatform platform, int count) {
        return IntStream.range(0, count)
            .mapToObj(index -> new MemberPushToken(
                (long) index,
                1L,
                "device-" + index,
                platform,
                "fcm-token-" + index,
                LocalDateTime.now()
            ))
            .toList();
    }
}
