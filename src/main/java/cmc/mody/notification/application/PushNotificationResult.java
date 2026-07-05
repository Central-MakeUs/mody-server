package cmc.mody.notification.application;

import java.util.List;

public record PushNotificationResult(List<String> invalidTokens) {
    public static PushNotificationResult empty() {
        return new PushNotificationResult(List.of());
    }

    public boolean hasInvalidTokens() {
        return !invalidTokens.isEmpty();
    }
}
