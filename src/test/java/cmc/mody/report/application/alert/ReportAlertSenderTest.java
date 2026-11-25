package cmc.mody.report.application.alert;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import cmc.mody.common.alert.SlackWebhookClient;
import cmc.mody.report.application.event.ReportFiledEvent;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ReportAlertSenderTest {
    private final ReportAlertSnapshotReader snapshots = org.mockito.Mockito.mock(ReportAlertSnapshotReader.class);
    private final SlackWebhookClient slack = org.mockito.Mockito.mock(SlackWebhookClient.class);

    @Test
    void sendsToConfiguredWebhook() {
        ReportFiledEvent event = ReportFiledEvent.record(100L, 900L);
        given(snapshots.read(event)).willReturn(new ReportAlertSnapshot(1, 1, "작성자"));

        sender().send(event);

        then(slack).should().sendPayload(eq("https://hooks.example.com/report"), anyMap());
    }

    @Test
    void slackFailureIsSwallowed() {
        ReportFiledEvent event = ReportFiledEvent.record(100L, 900L);
        given(snapshots.read(event)).willReturn(new ReportAlertSnapshot(1, 1, "작성자"));
        willThrow(new IllegalStateException("slack down")).given(slack)
            .sendPayload(eq("https://hooks.example.com/report"), anyMap());

        assertThatCode(() -> sender().send(event)).doesNotThrowAnyException();
    }

    private ReportAlertSender sender() {
        ReportAlertProperties properties = new ReportAlertProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl("https://hooks.example.com/report");
        return new ReportAlertSender(properties, snapshots, slack, new MockEnvironment());
    }
}
