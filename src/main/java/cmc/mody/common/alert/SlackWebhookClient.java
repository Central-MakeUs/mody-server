package cmc.mody.common.alert;

public interface SlackWebhookClient {
    void send(String webhookUrl, String text);
}
