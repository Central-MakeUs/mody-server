package cmc.mody.report.application.alert;

import cmc.mody.report.application.event.ReportFiledEvent;
import cmc.mody.report.application.event.ReportKind;
import cmc.mody.report.infrastructure.repository.CommentReportRepository;
import cmc.mody.report.infrastructure.repository.RecordReportRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReportAlertSnapshotReader {
    static final String WITHDRAWN = "탈퇴한 회원";
    private final RecordReportRepository recordReports;
    private final CommentReportRepository commentReports;
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public ReportAlertSnapshot read(ReportFiledEvent event) {
        boolean record = event.kind() == ReportKind.RECORD;
        long total = record ? recordReports.countByRecordIdAndDeletedAtIsNull(event.targetId())
            : commentReports.countByCommentIdAndDeletedAtIsNull(event.targetId());
        long pending = record ? recordReports.countByRecordIdAndResolutionIsNullAndDeletedAtIsNull(event.targetId())
            : commentReports.countByCommentIdAndResolutionIsNullAndDeletedAtIsNull(event.targetId());
        return new ReportAlertSnapshot(total, pending, authorNickname(event));
    }

    private String authorNickname(ReportFiledEvent event) {
        String table = event.kind() == ReportKind.RECORD ? "activity_record" : "record_comment";
        List<String> found = jdbc.query("""
            SELECT m.nickname, m.status, m.deleted_at
            FROM %s t LEFT JOIN member m ON m.id = t.member_id
            WHERE t.id = :id
            """.formatted(table), new MapSqlParameterSource("id", event.targetId()), (rs, n) ->
            "ACTIVE".equals(rs.getString("status")) && rs.getTimestamp("deleted_at") == null
                ? rs.getString("nickname") : WITHDRAWN);
        return found.isEmpty() ? null : found.getFirst();
    }
}
