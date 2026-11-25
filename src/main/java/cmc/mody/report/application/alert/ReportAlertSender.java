package cmc.mody.report.application.alert;

import cmc.mody.common.alert.SlackWebhookClient;
import cmc.mody.report.application.event.ReportFiledEvent;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** 신고 알림을 별도 스레드에서 보낸다. 조회·전송 실패는 로그만 남기고 신고에는 영향을 주지 않는다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAlertSender {
    private final ReportAlertProperties properties;
    private final ReportAlertSnapshotReader snapshots;
    private final SlackWebhookClient slack;
    private final Environment environment;

    @Async("reportAlertExecutor")
    public void send(ReportFiledEvent event) {
        try {
            ReportAlertSnapshot snapshot = snapshots.read(event);
            slack.sendPayload(properties.getWebhookUrl(), ReportAlertMessage.build(event, snapshot,
                environmentLabel(), properties.getConsoleBaseUrl(), properties.isButtonsEnabled()));
            log.info("report alert sent kind={} targetId={} reportId={}",
                event.kind(), event.targetId(), event.reportId());
        } catch (Exception exception) {
            log.warn("report alert failed kind={} targetId={} reportId={} reason={}",
                event.kind(), event.targetId(), event.reportId(), exception.toString());
        }
    }

    private String environmentLabel() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? "local" : String.join(",", Arrays.asList(profiles));
    }
}
