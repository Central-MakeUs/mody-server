package cmc.mody.report.application.alert;

import cmc.mody.report.application.event.ReportFiledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 신고 저장이 커밋된 뒤에만 Slack 알림을 예약한다. 롤백되면 호출되지 않는다.
 * 여기서 난 예외는 이미 커밋된 신고 요청을 실패로 바꿀 수 있으므로 모두 삼킨다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportAlertListener {
    private final ReportAlertProperties properties;
    private final ReportAlertThrottle throttle;
    private final ReportAlertSender sender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportFiled(ReportFiledEvent event) {
        try {
            if (!properties.isActive()) {
                return;
            }
            if (!throttle.tryAcquire(event.groupKey())) {
                log.info("report alert grouped kind={} targetId={} reportId={}",
                    event.kind(), event.targetId(), event.reportId());
                return;
            }
            sender.send(event);
        } catch (RuntimeException exception) {
            log.warn("report alert failed kind={} targetId={} reportId={} reason={}",
                event.kind(), event.targetId(), event.reportId(), exception.toString());
        }
    }
}
