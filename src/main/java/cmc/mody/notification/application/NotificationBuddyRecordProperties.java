package cmc.mody.notification.application;

import java.time.LocalTime;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.buddy-record")
public class NotificationBuddyRecordProperties {
    private boolean enabled = false;
    private LocalTime quietStart = LocalTime.MIDNIGHT;
    private LocalTime quietEnd = LocalTime.of(5, 0);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalTime getQuietStart() {
        return quietStart;
    }

    public void setQuietStart(LocalTime quietStart) {
        this.quietStart = quietStart;
    }

    public LocalTime getQuietEnd() {
        return quietEnd;
    }

    public void setQuietEnd(LocalTime quietEnd) {
        this.quietEnd = quietEnd;
    }

    public boolean isQuietTime(LocalTime time) {
        if (quietStart.isBefore(quietEnd)) {
            return !time.isBefore(quietStart) && time.isBefore(quietEnd);
        }
        return !time.isBefore(quietStart) || time.isBefore(quietEnd);
    }
}
