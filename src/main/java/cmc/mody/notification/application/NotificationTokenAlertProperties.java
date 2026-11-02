package cmc.mody.notification.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.token-alert")
public class NotificationTokenAlertProperties {
    private boolean enabled = true;
    private int threshold = 20;
    private Duration window = Duration.ofMinutes(10);
    private Duration cooldown = Duration.ofHours(1);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public Duration getCooldown() {
        return cooldown;
    }

    public void setCooldown(Duration cooldown) {
        this.cooldown = cooldown;
    }
}
