package cmc.mody.common.alert;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientSlackWebhookClient implements SlackWebhookClient {
    private final RestClient restClient = RestClient.create();

    @Override
    public void send(String webhookUrl, String text) {
        restClient.post()
            .uri(webhookUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("text", text))
            .retrieve()
            .toBodilessEntity();
    }

    @Override
    public void sendPayload(String webhookUrl, Map<String, Object> payload) {
        restClient.post()
            .uri(webhookUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .toBodilessEntity();
    }
}
