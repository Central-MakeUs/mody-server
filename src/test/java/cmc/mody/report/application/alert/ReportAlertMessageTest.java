package cmc.mody.report.application.alert;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.report.application.event.ReportFiledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReportAlertMessageTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void recordAlertHasCountsLinkAndButtonsButNoReporterOrContent() throws Exception {
        Map<String, Object> payload = ReportAlertMessage.build(ReportFiledEvent.record(100L, 900L),
            new ReportAlertSnapshot(3, 2, "작성<자>"), "dev", "https://dev-mody.store/", true);
        String json = mapper.writeValueAsString(payload);

        assertThat(json).contains("[dev] 기록 신고", "기록 `100`", "3건", "2건", "앱에서 받지 않음",
            "작성&lt;자&gt;", "https://dev-mody.store/admin/reports?kind=records", "신고 ID `900`");
        assertThat(json).contains("\"action_id\":\"report_hide\"", "\"action_id\":\"report_dismiss\"",
            "\"value\":\"RECORD:100\"", "\"confirm\"");
        assertThat(payload.get("text")).isEqualTo("[dev] 기록 신고: 기록 100 · 누적 3건 · 대기 2건");
    }

    @Test
    void commentAlertWithoutButtonsOrConsoleLink() throws Exception {
        String json = mapper.writeValueAsString(ReportAlertMessage.build(ReportFiledEvent.comment(200L, 100L, 901L),
            new ReportAlertSnapshot(1, 1, null), "prod", "", false));

        assertThat(json).contains("[prod] 댓글 신고", "댓글 `200` (기록 `100`)");
        assertThat(json).doesNotContain("report_hide", "admin/reports", "작성자");
    }
}
