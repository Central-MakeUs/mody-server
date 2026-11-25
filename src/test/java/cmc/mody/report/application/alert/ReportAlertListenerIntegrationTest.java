package cmc.mody.report.application.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import cmc.mody.report.application.event.ReportFiledEvent;
import cmc.mody.report.domain.RecordReport;
import cmc.mody.report.infrastructure.repository.RecordReportRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

/** 커밋 후에만 알리고, 알림 실패가 신고 저장을 실패시키지 않는지 실제 트랜잭션으로 확인한다. */
@SpringBootTest(properties = {
    "report-alert.slack.enabled=true",
    "report-alert.slack.webhook-url=https://hooks.example.com/report",
    "report-alert.slack.group-window=10m"
})
class ReportAlertListenerIntegrationTest {
    private static final long REPORT_ID = 9_800_001L;
    @Autowired private TransactionTemplate transactions;
    @Autowired private ApplicationEventPublisher events;
    @Autowired private RecordReportRepository reports;
    @MockitoBean private ReportAlertSender sender;

    @AfterEach
    void cleanup() {
        reports.findById(REPORT_ID).ifPresent(reports::delete);
    }

    @Test
    void rolledBackReportIsNotAlerted() {
        ReportFiledEvent event = ReportFiledEvent.record(9_800_100L, REPORT_ID);
        transactions.executeWithoutResult(status -> {
            events.publishEvent(event);
            status.setRollbackOnly();
        });

        then(sender).should(never()).send(any());
    }

    @Test
    void committedReportIsAlertedOnceAndRepeatedReportsOnSameTargetAreGrouped() {
        ReportFiledEvent first = ReportFiledEvent.record(9_800_200L, 1L);
        ReportFiledEvent second = ReportFiledEvent.record(9_800_200L, 2L);
        transactions.executeWithoutResult(status -> events.publishEvent(first));
        transactions.executeWithoutResult(status -> events.publishEvent(second));

        then(sender).should(times(1)).send(first);
        then(sender).should(never()).send(second);
    }

    @Test
    void slackFailureDoesNotFailReportSave() {
        willThrow(new IllegalStateException("slack down")).given(sender).send(any());

        assertThatCode(() -> transactions.executeWithoutResult(status -> {
            reports.save(new RecordReport(REPORT_ID, 1L, 9_800_300L));
            events.publishEvent(ReportFiledEvent.record(9_800_300L, REPORT_ID));
        })).doesNotThrowAnyException();

        assertThat(reports.findById(REPORT_ID)).isPresent();
        then(sender).should().send(any());
    }
}
