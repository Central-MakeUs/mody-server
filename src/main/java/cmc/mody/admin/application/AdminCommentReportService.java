package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import cmc.mody.report.domain.CommentReport;
import cmc.mody.report.domain.RecordReportResolution;
import cmc.mody.report.infrastructure.repository.CommentReportRepository;
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
 * 신고된 댓글을 댓글 단위로 묶어 보여주고, 숨김(댓글 삭제) 또는 기각으로 처리한다. 기록 신고와 같은 흐름이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommentReportService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Set<String> STATES = Set.of("PENDING", "RESOLVED", "ALL");
    private static final int REPORTER_PREVIEW = 5;
    private static final String WITHDRAWN = "탈퇴한 회원";
    private final NamedParameterJdbcTemplate jdbc;
    private final CommentReportRepository reports;
    private final RecordCommentRepository comments;

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
            having.append(" AND SUM(CASE WHEN cr.resolution IS NULL THEN 1 ELSE 0 END) > 0");
        } else if ("RESOLVED".equals(normalized)) {
            having.append(" AND SUM(CASE WHEN cr.resolution IS NULL THEN 1 ELSE 0 END) = 0");
        }
        if (StringUtils.hasText(cursor)) {
            Cursor decoded = decode(cursor);
            having.append(" AND (MAX(cr.created_at) < :cursorAt"
                + " OR (MAX(cr.created_at) = :cursorAt AND cr.comment_id < :cursorId))");
            params.addValue("cursorAt", decoded.lastReportedAt()).addValue("cursorId", decoded.commentId());
        }
        String sql = """
            SELECT cr.comment_id,
                COUNT(*) AS report_count,
                SUM(CASE WHEN cr.resolution IS NULL THEN 1 ELSE 0 END) AS pending_count,
                MIN(cr.created_at) AS first_reported_at,
                MAX(cr.created_at) AS last_reported_at,
                MAX(cr.resolved_at) AS resolved_at
            FROM comment_report cr WHERE cr.status = 'ACTIVE' AND cr.deleted_at IS NULL
            GROUP BY cr.comment_id
            """ + having + " ORDER BY last_reported_at DESC, cr.comment_id DESC LIMIT :limit";
        List<Row> rows = jdbc.query(sql, params, (rs, n) -> new Row(rs.getLong("comment_id"),
            rs.getLong("report_count"), rs.getLong("pending_count"), time(rs, "first_reported_at"),
            time(rs, "last_reported_at"), time(rs, "resolved_at")));
        boolean hasMore = rows.size() > size;
        List<Row> page = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? encode(page.getLast()) : null;
        return new Page(enrich(page), next, hasMore);
    }

    public Summary summary() {
        var params = new MapSqlParameterSource("week", LocalDate.now(KST).minusDays(6).atStartOfDay());
        return jdbc.queryForObject("""
            SELECT
                COUNT(DISTINCT CASE WHEN cr.resolution IS NULL THEN cr.comment_id END) AS pending_comments,
                SUM(CASE WHEN cr.resolution IS NULL THEN 1 ELSE 0 END) AS pending_reports,
                SUM(CASE WHEN cr.resolution = 'HIDDEN' AND cr.resolved_at >= :week THEN 1 ELSE 0 END) AS hidden_week,
                SUM(CASE WHEN cr.resolution = 'DISMISSED' AND cr.resolved_at >= :week THEN 1 ELSE 0 END)
                    AS dismissed_week
            FROM comment_report cr WHERE cr.status = 'ACTIVE' AND cr.deleted_at IS NULL
            """, params, (rs, n) -> new Summary(rs.getLong("pending_comments"), rs.getLong("pending_reports"),
            rs.getLong("hidden_week"), rs.getLong("dismissed_week")));
    }

    /** 댓글을 삭제하고 대기 중인 신고를 HIDDEN으로 종결한다. 이미 삭제된 댓글이어도 신고는 종결한다. */
    @Transactional
    public Resolution hide(long commentId, String actor) {
        List<CommentReport> pending = pendingReports(commentId);
        boolean deleted = comments.findById(commentId)
            .filter(RecordComment::isActive)
            .map(comment -> {
                comment.delete();
                return true;
            })
            .orElse(false);
        resolve(pending, RecordReportResolution.HIDDEN, actor);
        return new Resolution(Long.toString(commentId), RecordReportResolution.HIDDEN.name(), pending.size(),
            deleted);
    }

    @Transactional
    public Resolution dismiss(long commentId, String actor) {
        List<CommentReport> pending = pendingReports(commentId);
        resolve(pending, RecordReportResolution.DISMISSED, actor);
        return new Resolution(Long.toString(commentId), RecordReportResolution.DISMISSED.name(), pending.size(),
            false);
    }

    private List<CommentReport> pendingReports(long commentId) {
        List<CommentReport> pending = reports.findByCommentIdAndResolutionIsNullAndDeletedAtIsNull(commentId);
        if (pending.isEmpty()) {
            throw new GeneralException(ErrorStatus.NOT_FOUND);
        }
        return pending;
    }

    private void resolve(List<CommentReport> pending, RecordReportResolution resolution, String actor) {
        LocalDateTime now = LocalDateTime.now();
        pending.forEach(report -> report.resolve(resolution, actor, now));
    }

    private List<Item> enrich(List<Row> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        var ids = new MapSqlParameterSource("ids", rows.stream().map(Row::commentId).toList());
        Map<Long, Item> found = new HashMap<>();
        jdbc.query("""
            SELECT c.id, c.record_id, c.group_id, c.content, c.emoji_id, c.created_at,
                c.status, c.deleted_at, c.member_id, m.nickname, m.status AS member_status,
                m.deleted_at AS member_deleted_at, g.name AS group_name,
                r.record_type, r.menu, r.exercise_name
            FROM record_comment c
            LEFT JOIN member m ON m.id = c.member_id
            LEFT JOIN mody_group g ON g.id = c.group_id
            LEFT JOIN activity_record r ON r.id = c.record_id
            WHERE c.id IN (:ids)
            """, ids, rs -> {
            boolean memberActive = "ACTIVE".equals(rs.getString("member_status"))
                && rs.getTimestamp("member_deleted_at") == null;
            String type = rs.getString("record_type");
            found.put(rs.getLong("id"), new Item(Long.toString(rs.getLong("id")),
                Long.toString(rs.getLong("record_id")), type,
                "MEAL".equals(type) ? rs.getString("menu") : rs.getString("exercise_name"),
                Long.toString(rs.getLong("group_id")), rs.getString("group_name"),
                rs.getString("content"), rs.getString("emoji_id"), time(rs, "created_at"),
                "ACTIVE".equals(rs.getString("status")) && rs.getTimestamp("deleted_at") == null,
                Long.toString(rs.getLong("member_id")), memberActive ? rs.getString("nickname") : WITHDRAWN,
                0, 0, null, null, null, List.of(), null));
        });
        Map<Long, List<AdminReportService.Reporter>> reporters = new HashMap<>();
        Map<Long, String> resolutions = new HashMap<>();
        jdbc.query("""
            SELECT cr.comment_id, cr.reporter_member_id, cr.created_at, cr.resolution, m.nickname,
                m.status AS member_status, m.deleted_at AS member_deleted_at
            FROM comment_report cr LEFT JOIN member m ON m.id = cr.reporter_member_id
            WHERE cr.comment_id IN (:ids) AND cr.status = 'ACTIVE' AND cr.deleted_at IS NULL
            ORDER BY cr.created_at DESC
            """, ids, rs -> {
            long commentId = rs.getLong("comment_id");
            List<AdminReportService.Reporter> list = reporters.computeIfAbsent(commentId, key -> new ArrayList<>());
            if (list.size() < REPORTER_PREVIEW) {
                boolean active = "ACTIVE".equals(rs.getString("member_status"))
                    && rs.getTimestamp("member_deleted_at") == null;
                list.add(new AdminReportService.Reporter(Long.toString(rs.getLong("reporter_member_id")),
                    active ? rs.getString("nickname") : WITHDRAWN, time(rs, "created_at")));
            }
            if (rs.getString("resolution") != null) {
                resolutions.putIfAbsent(commentId, rs.getString("resolution"));
            }
        });
        return rows.stream().map(row -> {
            Item base = found.get(row.commentId());
            String resolution = row.pendingCount() > 0 ? null : resolutions.get(row.commentId());
            List<AdminReportService.Reporter> who = reporters.getOrDefault(row.commentId(), List.of());
            if (base == null) {
                return new Item(Long.toString(row.commentId()), null, null, null, null, null, null, null, null,
                    false, null, null, row.reportCount(), row.pendingCount(), row.firstReportedAt(),
                    row.lastReportedAt(), row.resolvedAt(), who, resolution);
            }
            return new Item(base.commentId(), base.recordId(), base.recordType(), base.recordTitle(),
                base.groupId(), base.groupName(), base.content(), base.emojiId(), base.createdAt(),
                base.commentActive(), base.authorId(), base.authorNickname(), row.reportCount(),
                row.pendingCount(), row.firstReportedAt(), row.lastReportedAt(), row.resolvedAt(), who,
                resolution);
        }).toList();
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String encode(Row row) {
        String raw = row.lastReportedAt() + "|" + row.commentId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            return new Cursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
    }

    private record Cursor(LocalDateTime lastReportedAt, long commentId) { }

    private record Row(long commentId, long reportCount, long pendingCount, LocalDateTime firstReportedAt,
        LocalDateTime lastReportedAt, LocalDateTime resolvedAt) { }

    /** recordId가 null이면 댓글 자체를 찾을 수 없는 경우다. content가 비어 있으면 emojiId만 있는 이모지 댓글이다. */
    public record Item(String commentId, String recordId, String recordType, String recordTitle, String groupId,
        String groupName, String content, String emojiId, LocalDateTime createdAt, boolean commentActive,
        String authorId, String authorNickname, long reportCount, long pendingCount, LocalDateTime firstReportedAt,
        LocalDateTime lastReportedAt, LocalDateTime resolvedAt, List<AdminReportService.Reporter> reporters,
        String resolution) { }

    public record Page(List<Item> items, String nextCursor, boolean hasMore) { }

    public record Summary(long pendingComments, long pendingReports, long hiddenLast7Days,
        long dismissedLast7Days) { }

    public record Resolution(String commentId, String resolution, int resolvedReports, boolean commentDeleted) { }
}
