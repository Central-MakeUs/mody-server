package cmc.mody.common.alert;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerErrorAlertSenderTest {
    @Mock
    private SlackWebhookClient slackWebhookClient;

    @Test
    void ignoreSlackFailure() {
        ServerErrorAlertSender sender = new ServerErrorAlertSender(slackWebhookClient);
        willThrow(new RuntimeException("slack down")).given(slackWebhookClient)
            .send("https://hooks.slack.test/error", "message");

        assertThatCode(() -> sender.send("https://hooks.slack.test/error", "message"))
            .doesNotThrowAnyException();
    }
}
