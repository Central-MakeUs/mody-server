package cmc.mody.report.application.alert;

import cmc.mody.report.application.event.ReportFiledEvent;
import cmc.mody.report.application.event.ReportKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 신고 알림 Slack 메시지(Block Kit). 신고자 정보, 기록 제목·메뉴, 댓글 본문, 이미지는 넣지 않는다.
 * 버튼 값은 {@code RECORD:<id>} / {@code COMMENT:<id>}이고 mody-slack-bot이 받아 처리한다.
 */
public final class ReportAlertMessage {
    public static final String HIDE_ACTION = "report_hide";
    public static final String DISMISS_ACTION = "report_dismiss";
    static final String REASON_NOT_COLLECTED = "앱에서 받지 않음";

    private ReportAlertMessage() {
    }

    public static Map<String, Object> build(ReportFiledEvent event, ReportAlertSnapshot snapshot, String environment,
        String consoleBaseUrl, boolean buttons) {
        boolean record = event.kind() == ReportKind.RECORD;
        String kindLabel = record ? "기록" : "댓글";
        String title = "[%s] %s 신고".formatted(environment, kindLabel);
        String target = record ? "기록 `%d`".formatted(event.targetId())
            : "댓글 `%d` (기록 `%d`)".formatted(event.targetId(), event.recordId());

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(mrkdwn("*대상*\n" + target));
        fields.add(mrkdwn("*신고 사유*\n" + REASON_NOT_COLLECTED));
        fields.add(mrkdwn("*누적 신고*\n%d건".formatted(snapshot.totalReports())));
        fields.add(mrkdwn("*처리 대기*\n%d건".formatted(snapshot.pendingReports())));
        if (snapshot.authorNickname() != null) {
            fields.add(mrkdwn("*작성자*\n" + escape(snapshot.authorNickname())));
        }

        List<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(Map.of("type", "header",
            "text", Map.of("type", "plain_text", "text", ":rotating_light: " + title)));
        blocks.add(Map.of("type", "section", "fields", fields));
        if (StringUtils.hasText(consoleBaseUrl)) {
            String url = trimSlash(consoleBaseUrl) + "/admin/reports?kind=" + (record ? "records" : "comments");
            blocks.add(Map.of("type", "context", "elements",
                List.of(mrkdwn("신고 ID `%d` · <%s|콘솔에서 보기>".formatted(event.reportId(), url)))));
        } else {
            blocks.add(Map.of("type", "context", "elements", List.of(mrkdwn("신고 ID `%d`".formatted(event.reportId())))));
        }
        if (buttons) {
            String value = event.kind().name() + ":" + event.targetId();
            blocks.add(Map.of("type", "actions", "block_id", "report_actions", "elements", List.of(
                button(HIDE_ACTION, "숨김", value, "danger", confirm(kindLabel)),
                button(DISMISS_ACTION, "기각", value, null, null))));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("text", "%s: %s · 누적 %d건 · 대기 %d건".formatted(title, target.replace("`", ""),
            snapshot.totalReports(), snapshot.pendingReports()));
        payload.put("blocks", blocks);
        return payload;
    }

    private static Map<String, Object> button(String actionId, String label, String value, String style,
        Map<String, Object> confirm) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("type", "button");
        button.put("action_id", actionId);
        button.put("text", Map.of("type", "plain_text", "text", label));
        button.put("value", value);
        if (style != null) {
            button.put("style", style);
        }
        if (confirm != null) {
            button.put("confirm", confirm);
        }
        return button;
    }

    private static Map<String, Object> confirm(String kindLabel) {
        return Map.of(
            "title", Map.of("type", "plain_text", "text", kindLabel + " 숨김"),
            "text", Map.of("type", "mrkdwn", "text", kindLabel + "을(를) 삭제하고 대기 중인 신고를 닫습니다. 되돌릴 수 없습니다."),
            "confirm", Map.of("type", "plain_text", "text", "숨김"),
            "deny", Map.of("type", "plain_text", "text", "취소"),
            "style", "danger");
    }

    private static Map<String, Object> mrkdwn(String text) {
        return Map.of("type", "mrkdwn", "text", text);
    }

    /** Slack mrkdwn 제어 문자만 바꾼다. */
    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
