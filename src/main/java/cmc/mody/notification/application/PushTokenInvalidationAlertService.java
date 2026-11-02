package cmc.mody.notification.application;

import cmc.mody.common.alert.ServerErrorAlertProperties;
import cmc.mody.common.alert.ServerErrorAlertSender;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.PushPlatform;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * FCM이 무효로 응답해 비활성화한 토큰이 짧은 시간에 몰리면 Slack으로 알린다.
 * 토큰이 없어도 알림은 SENT로 처리되므로, 대량 비활성화는 이 알림 없이는 드러나지 않는다.
 */
@Service
@RequiredArgsConstructor
public class PushTokenInvalidationAlertService {
    private final NotificationTokenAlertProperties tokenAlertProperties;
    private final ServerErrorAlertProperties alertProperties;
    private final ServerErrorAlertSender alertSender;

    private final Deque<DisabledToken> disabledTokens = new ArrayDeque<>();
    private Clock clock = Clock.systemUTC();
    private Instant lastAlertedAt;

    void useClock(Clock clock) {
        this.clock = clock;
    }

    public synchronized void record(List<MemberPushToken> tokens) {
        if (tokens.isEmpty() || !isEnabled()) {
            return;
        }

        Instant now = clock.instant();
        tokens.forEach(token -> disabledTokens.addLast(new DisabledToken(now, token.getPlatform())));
        Instant windowStart = now.minus(tokenAlertProperties.getWindow());
        while (!disabledTokens.isEmpty() && disabledTokens.peekFirst().disabledAt().isBefore(windowStart)) {
            disabledTokens.pollFirst();
        }

        if (disabledTokens.size() < tokenAlertProperties.getThreshold() || isCoolingDown(now)) {
            return;
        }
        lastAlertedAt = now;
        alertSender.send(alertProperties.getWebhookUrl(), format());
    }

    private boolean isEnabled() {
        return tokenAlertProperties.isEnabled()
            && alertProperties.isEnabled()
            && StringUtils.hasText(alertProperties.getWebhookUrl());
    }

    private boolean isCoolingDown(Instant now) {
        return lastAlertedAt != null && now.isBefore(lastAlertedAt.plus(tokenAlertProperties.getCooldown()));
    }

    private String format() {
        Map<PushPlatform, Long> countByPlatform = disabledTokens.stream()
            .collect(Collectors.groupingBy(
                DisabledToken::platform,
                () -> new EnumMap<>(PushPlatform.class),
                Collectors.counting()
            ));
        String platforms = countByPlatform.entrySet().stream()
            .map(entry -> entry.getKey() + " " + entry.getValue())
            .collect(Collectors.joining(" / "));

        return """
            :rotating_light: *Push Tokens Disabled In Bulk*
            *Count*: `%d` in last `%d` min (threshold `%d`)
            *Platform*: `%s`
            *Reason*: FCM returned UNREGISTERED or INVALID_ARGUMENT
            *Check*: app token re-registration, APNs key, Firebase project
            """.formatted(
            disabledTokens.size(),
            tokenAlertProperties.getWindow().toMinutes(),
            tokenAlertProperties.getThreshold(),
            platforms
        );
    }

    private record DisabledToken(Instant disabledAt, PushPlatform platform) {
    }
}
