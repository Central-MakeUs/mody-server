package cmc.mody.common.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/**
 * 등록되는 모든 작업을 {@link ActiveColorSchedulingGate}로 감싸 위임한다.
 */
public class GatedTaskScheduler implements TaskScheduler {
    private final TaskScheduler delegate;
    private final ActiveColorSchedulingGate gate;

    public GatedTaskScheduler(TaskScheduler delegate, ActiveColorSchedulingGate gate) {
        this.delegate = delegate;
        this.gate = gate;
    }

    @Override
    public Clock getClock() {
        return delegate.getClock();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return delegate.schedule(gate.wrap(task), trigger);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
        return delegate.schedule(gate.wrap(task), startTime);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
        return delegate.scheduleAtFixedRate(gate.wrap(task), startTime, period);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
        return delegate.scheduleAtFixedRate(gate.wrap(task), period);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
        return delegate.scheduleWithFixedDelay(gate.wrap(task), startTime, delay);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
        return delegate.scheduleWithFixedDelay(gate.wrap(task), delay);
    }
}
