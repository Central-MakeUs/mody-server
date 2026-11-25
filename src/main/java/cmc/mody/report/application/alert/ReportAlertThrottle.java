package cmc.mody.report.application.alert;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 같은 대상(기록 또는 댓글)의 신고 알림을 묶음 시간 안에 한 번만 보내게 한다.
 * 첫 신고는 바로 보내고, 묶음 시간 안의 같은 대상 신고는 건너뛴다. 메모리에만 두므로 재시작하면 비워진다.
 */
@Component
@RequiredArgsConstructor
public class ReportAlertThrottle {
    private final ReportAlertProperties properties;
    private final Map<String, Instant> lastAlertedAt = new HashMap<>();
    private Clock clock = Clock.systemUTC();

    void useClock(Clock clock) {
        this.clock = clock;
    }

    /** 지금 알려야 하면 true를 돌려주고 시각을 기록한다. */
    public synchronized boolean tryAcquire(String key) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(properties.getGroupWindow());
        lastAlertedAt.values().removeIf(at -> !at.isAfter(windowStart));
        if (lastAlertedAt.containsKey(key)) {
            return false;
        }
        lastAlertedAt.put(key, now);
        return true;
    }
}
