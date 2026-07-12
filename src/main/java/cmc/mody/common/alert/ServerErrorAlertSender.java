package cmc.mody.common.alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerErrorAlertSender {
    private final SlackWebhookClient slackWebhookClient;

    @Async("serverErrorAlertExecutor")
    public void send(String webhookUrl, String text) {
        try {
            slackWebhookClient.send(webhookUrl, text);
        } catch (Exception e) {
            log.warn("Failed to send server error alert to Slack: {}", e.getMessage());
        }
    }
}
