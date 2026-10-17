package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.upload.ImageUrlResolver;
import cmc.mody.record.application.AdminRecordService;
import cmc.mody.report.domain.RecordReport;
import cmc.mody.report.domain.RecordReportResolution;
import cmc.mody.report.infrastructure.repository.RecordReportRepository;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 신고된 활동 기록을 기록 단위로 묶어 보여주고, 숨김(기록 삭제) 또는 기각으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Set<String> STATES = Set.of("PENDING", "RESOLVED", "ALL");
    private static final String LIVE_REPORT = "rr.status = 'ACTIVE' AND rr.deleted_at IS NULL";
    private static final int REPORTER_PREVIEW = 5;
    private final NamedParameterJdbcTemplate jdbc;
    private final ImageUrlResolver images;
    private final RecordReportRepository reports;
    private final AdminRecordService records;

    public Page list(String state, String cursor, int size) {
        if (size < 1 || size > 100) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        String normalized = StringUtils.hasText(state) ? state.trim().toUpperCase(Locale.ROOT) : "PENDING";
        if (!STATES.contains(normalized)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        var params = new MapSqlParameterSource().addValue("limit", size + 1);
        StringBuilder having = new StringBuilder(" HAVING 1 = 1");
        if ("PENDING".equals(normalized)) {
            having.append(" AND SUM(CASE WHEN rr.resolution IS NULL THEN 1 ELSE 0 END) > 0");
        } else if ("RESOLVED".equals(normalized)) {
            having.append(" AND SUM(CASE WHEN rr.resolution IS NULL THEN 1 ELSE 0 END) = 0");
        }
        if (StringUtils.hasText(cursor)) {
            Cursor decoded = decode(cursor);
            having.append(" AND (MAX(rr.created_at) < :cursorAt"
                + " OR (MAX(rr.created_at) = :cursorAt AND rr.record_id < :cursorId))");
            params.addValue("cursorAt", decoded.lastReportedAt()).addValue("cursorId", decoded.recordId());
        }
        String sql = """
            SELECT rr.record_id,
                COUNT(*) AS report_count,
                SUM(CASE WHEN rr.resolution IS NULL THEN 1 ELSE 0 END) AS pending_count,
                MIN(rr.created_at) AS first_reported_at,
                MAX(rr.created_at) AS last_reported_at,
                MAX(rr.resolved_at) AS resolved_at
            FROM record_report rr WHERE %s
            GROUP BY rr.record_id
            """.formatted(LIVE_REPORT) + having + " ORDER BY last_reported_at DESC, rr.record_id DESC LIMIT :limit";
        List<Row> rows = jdbc.query(sql, params, (rs, n) -> new Row(rs.getLong("record_id"),
            rs.getLong("report_count"), rs.getLong("pending_count"), time(rs, "first_reported_at"),
            time(rs, "last_reported_at"), time(rs, "resolved_at")));
        boolean hasMore = rows.size() > size;
        List<Row> page = hasMore ? rows.subList(0, size) : rows;
        List<Item> items = enrich(page);
        String next = hasMore ? encode(page.getLast()) : null;
        return new Page(items, next, hasMore);
    }

    public Summary summary() {
        var params = new MapSqlParameterSource("week", LocalDate.now(KST).minusDays(6).atStartOfDay());
        return jdbc.queryForObject("""
            SELECT
                COUNT(DISTINCT CASE WHEN rr.resolution IS NULL THEN rr.record_id END) AS pending_records,
                SUM(CASE WHEN rr.resolution IS NULL THEN 1 ELSE 0 END) AS pending_reports,
                SUM(CASE WHEN rr.resolution = 'HIDDEN' AND rr.resolved_at >= :week THEN 1 ELSE 0 END) AS hidden_week,
                SUM(CASE WHEN rr.resolution = 'DISMISSED' AND rr.resolved_at >= :week THEN 1 ELSE 0 END)
                    AS dismissed_week
            FROM record_report rr WHERE rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            """, params, (rs, n) -> new Summary(rs.getLong("pending_records"), rs.getLong("pending_reports"),
            rs.getLong("hidden_week"), rs.getLong("dismissed_week")));
    }

    /** 기록을 삭제 처리하고 대기 중인 신고를 HIDDEN으로 종결한다. 이미 삭제된 기록이어도 신고는 종결한다. */
    @Transactional
    public Resolution hide(long recordId, String actor) {
        List<RecordReport> pending = pendingReports(recordId);
        boolean deleted = records.deleteRecord(recordId);
        resolve(pending, RecordReportResolution.HIDDEN, actor);
        return new Resolution(Long.toString(recordId), RecordReportResolution.HIDDEN.name(), pending.size(),
            deleted);
    }

    @Transactional
    public Resolution dismiss(long recordId, String actor) {
        List<RecordReport> pending = pendingReports(recordId);
        resolve(pending, RecordReportResolution.DISMISSED, actor);
        return new Resolution(Long.toString(recordId), RecordReportResolution.DISMISSED.name(), pending.size(),
            false);
    }

    private List<RecordReport> pendingReports(long recordId) {
        List<RecordReport> pending = reports.findByRecordIdAndResolutionIsNullAndDeletedAtIsNull(recordId);
        if (pending.isEmpty()) {
            throw new GeneralException(ErrorStatus.NOT_FOUND);
        }
        return pending;
    }

    private void resolve(List<RecordReport> pending, RecordReportResolution resolution, String actor) {
        LocalDateTime now = LocalDateTime.now();
        pending.forEach(report -> report.resolve(resolution, actor, now));
    }

    private List<Item> enrich(List<Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        var ids = new MapSqlParameterSource("ids", rows.stream().map(Row::recordId).toList());
        Map<Long, Item> records = new HashMap<>();
        jdbc.query("""
            SELECT r.id, r.record_type, r.menu, r.exercise_name, r.image_key, r.uploaded_at,
                r.status, r.deleted_at, r.member_id, m.nickname, m.status AS member_status,
                m.deleted_at AS member_deleted_at
            FROM activity_record r LEFT JOIN member m ON m.id = r.member_id
            WHERE r.id IN (:ids)
            """, ids, rs -> {
            boolean memberActive = "ACTIVE".equals(rs.getString("member_status"))
                && rs.getTimestamp("member_deleted_at") == null;
            String type = rs.getString("record_type");
            records.put(rs.getLong("id"), new Item(Long.toString(rs.getLong("id")), type,
                "MEAL".equals(type) ? rs.getString("menu") : rs.getString("exercise_name"),
                images.resolve(rs.getString("image_key")), time(rs, "uploaded_at"),
                "ACTIVE".equals(rs.getString("status")) && rs.getTimestamp("deleted_at") == null,
                Long.toString(rs.getLong("member_id")), memberActive ? rs.getString("nickname") : "탈퇴한 회원",
                0, 0, null, null, null, List.of(), null));
        });
        Map<Long, List<Reporter>> reporters = new HashMap<>();
        Map<Long, String> resolutions = new HashMap<>();
        jdbc.query("""
            SELECT rr.record_id, rr.reporter_member_id, rr.created_at, rr.resolution, m.nickname,
                m.status AS member_status, m.deleted_at AS member_deleted_at
            FROM record_report rr LEFT JOIN member m ON m.id = rr.reporter_member_id
            WHERE rr.record_id IN (:ids) AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL
            ORDER BY rr.created_at DESC
            """, ids, rs -> {
            long recordId = rs.getLong("record_id");
            List<Reporter> list = reporters.computeIfAbsent(recordId, key -> new ArrayList<>());
            if (list.size() < REPORTER_PREVIEW) {
                boolean active = "ACTIVE".equals(rs.getString("member_status"))
                    && rs.getTimestamp("member_deleted_at") == null;
                list.add(new Reporter(Long.toString(rs.getLong("reporter_member_id")),
                    active ? rs.getString("nickname") : "탈퇴한 회원", time(rs, "created_at")));
            }
            if (rs.getString("resolution") != null) {
                resolutions.putIfAbsent(recordId, rs.getString("resolution"));
            }
        });
        return rows.stream().map(row -> {
            Item base = records.get(row.recordId());
            String resolution = row.pendingCount() > 0 ? null : resolutions.get(row.recordId());
            if (base == null) {
                return new Item(Long.toString(row.recordId()), null, null, null, null, false, null, null,
                    row.reportCount(), row.pendingCount(), row.firstReportedAt(), row.lastReportedAt(),
                    row.resolvedAt(), reporters.getOrDefault(row.recordId(), List.of()), resolution);
            }
            return new Item(base.recordId(), base.recordType(), base.title(), base.imageUrl(), base.uploadedAt(),
                base.recordActive(), base.authorId(), base.authorNickname(), row.reportCount(), row.pendingCount(),
                row.firstReportedAt(), row.lastReportedAt(), row.resolvedAt(),
                reporters.getOrDefault(row.recordId(), List.of()), resolution);
        }).toList();
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String encode(Row row) {
        String raw = row.lastReportedAt() + "|" + row.recordId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Cursor decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            return new Cursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
    }

    private record Cursor(LocalDateTime lastReportedAt, long recordId) { }

    private record Row(long recordId, long reportCount, long pendingCount, LocalDateTime firstReportedAt,
        LocalDateTime lastReportedAt, LocalDateTime resolvedAt) { }

    public record Reporter(String memberId, String nickname, LocalDateTime reportedAt) { }

    /** recordType이 null이면 기록 자체를 찾을 수 없는 경우다. */
    public record Item(String recordId, String recordType, String title, String imageUrl, LocalDateTime uploadedAt,
        boolean recordActive, String authorId, String authorNickname, long reportCount, long pendingCount,
        LocalDateTime firstReportedAt, LocalDateTime lastReportedAt, LocalDateTime resolvedAt,
        List<Reporter> reporters, String resolution) { }

    public record Page(List<Item> items, String nextCursor, boolean hasMore) { }

    public record Summary(long pendingRecords, long pendingReports, long hiddenLast7Days,
        long dismissedLast7Days) { }

    public record Resolution(String recordId, String resolution, int resolvedReports, boolean recordDeleted) { }
}
