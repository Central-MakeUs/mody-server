package cmc.mody.report.application.alert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ReportAlertThrottleTest {
    private static final Instant T0 = Instant.parse("2026-11-25T01:00:00Z");

    @Test
    void sameTargetIsAlertedOnceWithinWindowAndAgainAfterIt() {
        ReportAlertThrottle throttle = throttle();
        throttle.useClock(Clock.fixed(T0, ZoneOffset.UTC));
        assertThat(throttle.tryAcquire("RECORD:1")).isTrue();
        assertThat(throttle.tryAcquire("RECORD:1")).isFalse();

        throttle.useClock(Clock.fixed(T0.plus(Duration.ofMinutes(9)), ZoneOffset.UTC));
        assertThat(throttle.tryAcquire("RECORD:1")).isFalse();

        throttle.useClock(Clock.fixed(T0.plus(Duration.ofMinutes(10)), ZoneOffset.UTC));
        assertThat(throttle.tryAcquire("RECORD:1")).isTrue();
    }

    @Test
    void differentTargetsAreAlertedSeparately() {
        ReportAlertThrottle throttle = throttle();
        throttle.useClock(Clock.fixed(T0, ZoneOffset.UTC));
        assertThat(throttle.tryAcquire("RECORD:1")).isTrue();
        assertThat(throttle.tryAcquire("RECORD:2")).isTrue();
        assertThat(throttle.tryAcquire("COMMENT:1")).isTrue();
    }

    private ReportAlertThrottle throttle() {
        ReportAlertProperties properties = new ReportAlertProperties();
        properties.setGroupWindow(Duration.ofMinutes(10));
        return new ReportAlertThrottle(properties);
    }
}
