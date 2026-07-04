package cmc.mody.notification.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.util.Map;

public final class NotificationPayload {
    private NotificationPayload() {
    }

    public static Long requireLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Integer integerValue) {
            return integerValue.longValue();
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        throw new GeneralException(ErrorStatus.NOTIFICATION_PAYLOAD_INVALID);
    }

    public static String requireString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new GeneralException(ErrorStatus.NOTIFICATION_PAYLOAD_INVALID);
    }
}
