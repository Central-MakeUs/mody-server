package cmc.mody.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class ActiveColorSchedulingGateTest {
    @TempDir
    Path tempDir;

    @Test
    void 설정이_없으면_항상_실행한다() {
        assertThat(new ActiveColorSchedulingGate("", "").isActive()).isTrue();
        assertThat(new ActiveColorSchedulingGate(tempDir.resolve("active").toString(), "").isActive()).isTrue();
    }

    @Test
    void 활성_색상과_같을_때만_실행한다() throws Exception {
        Path file = tempDir.resolve("active-color");
        Files.writeString(file, "green\n");

        assertThat(new ActiveColorSchedulingGate(file.toString(), "green").isActive()).isTrue();
        assertThat(new ActiveColorSchedulingGate(file.toString(), "blue").isActive()).isFalse();
    }

    @Test
    void 파일을_읽지_못하면_실행한다() {
        ActiveColorSchedulingGate gate = new ActiveColorSchedulingGate(tempDir.resolve("missing").toString(), "blue");

        assertThat(gate.isActive()).isTrue();
    }

    @Test
    void 비활성_중에도_반복_예약은_유지되고_활성화되면_실행된다() throws Exception {
        Path file = tempDir.resolve("active-color");
        Files.writeString(file, "green");
        ActiveColorSchedulingGate gate = new ActiveColorSchedulingGate(file.toString(), "blue");
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        GatedTaskScheduler gatedScheduler = new GatedTaskScheduler(scheduler, gate);
        AtomicInteger runs = new AtomicInteger();

        try {
            gatedScheduler.scheduleWithFixedDelay(runs::incrementAndGet, Duration.ofMillis(20));
            Thread.sleep(200);
            assertThat(runs.get()).isZero();

            Files.writeString(file, "blue");
            long deadline = System.currentTimeMillis() + 2000;
            while (runs.get() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertThat(runs.get()).isPositive();
        } finally {
            scheduler.shutdown();
        }
    }
}
