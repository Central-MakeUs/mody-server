package cmc.mody.common.alert;

import java.util.Map;

public interface SlackWebhookClient {
    void send(String webhookUrl, String text);

    /** Block Kit 메시지처럼 text 외 필드가 필요한 본문을 그대로 보낸다. */
    void sendPayload(String webhookUrl, Map<String, Object> payload);
}
