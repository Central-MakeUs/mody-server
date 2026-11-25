package cmc.mody.report.application.event;

/**
 * 새 신고가 저장됐다. 커밋된 뒤 Slack 알림(LLD-0040)에 쓰인다. 같은 신고자의 재신고는 발행하지 않는다.
 *
 * @param kind     신고 대상 종류
 * @param targetId 기록 ID 또는 댓글 ID
 * @param recordId 대상 기록 ID. 기록 신고면 targetId와 같다
 * @param reportId 저장된 신고 ID
 */
public record ReportFiledEvent(ReportKind kind, Long targetId, Long recordId, Long reportId) {
    public static ReportFiledEvent record(Long recordId, Long reportId) {
        return new ReportFiledEvent(ReportKind.RECORD, recordId, recordId, reportId);
    }

    public static ReportFiledEvent comment(Long commentId, Long recordId, Long reportId) {
        return new ReportFiledEvent(ReportKind.COMMENT, commentId, recordId, reportId);
    }

    public String groupKey() {
        return kind + ":" + targetId;
    }
}
