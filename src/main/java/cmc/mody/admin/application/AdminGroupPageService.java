package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 그룹 하나를 한 화면에서 보는 관리 페이지. 구성원, 초대 코드, 주간 챌린지, 최근 댓글을 모아 준다.
 * 기록 피드는 인증 피드 API(groupId 필터)를 그대로 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGroupPageService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String WITHDRAWN = "탈퇴한 회원";
    private static final String JOINED = """
        SELECT COUNT(*) FROM group_member gm WHERE gm.group_id = g.id AND gm.group_member_status = 'JOINED'
            AND gm.status = 'ACTIVE' AND gm.deleted_at IS NULL
        """;
    private static final String GROUP_RECORDS = """
        FROM activity_record_group arg JOIN activity_record r ON r.id = arg.record_id
            AND r.status = 'ACTIVE' AND r.deleted_at IS NULL
        WHERE arg.group_id = g.id AND arg.status = 'ACTIVE' AND arg.deleted_at IS NULL
        """;
    private final NamedParameterJdbcTemplate jdbc;

    public Page list(String keyword, String cursor, int size) {
        if (size < 1 || size > 100 || (keyword != null && keyword.length() > 100)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        LocalDateTime week = LocalDate.now(KST).minusDays(6).atStartOfDay();
        var params = new MapSqlParameterSource().addValue("limit", size + 1).addValue("week", week);
        StringBuilder where = new StringBuilder(" WHERE g.status = 'ACTIVE' AND g.deleted_at IS NULL");
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            params.addValue("search", "%" + trimmed.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%")
                .addValue("code", trimmed.toUpperCase(java.util.Locale.ROOT));
            where.append(" AND (g.name LIKE :search ESCAPE '!' OR g.code = :code");
            if (trimmed.matches("\\d{1,19}")) {
                params.addValue("groupId", Long.parseLong(trimmed));
                where.append(" OR g.id = :groupId");
            }
            where.append(")");
        }
        if (StringUtils.hasText(cursor)) {
            Cursor decoded = decode(cursor);
            where.append(" AND (g.created_at < :cursorAt OR (g.created_at = :cursorAt AND g.id < :cursorId))");
            params.addValue("cursorAt", decoded.createdAt()).addValue("cursorId", decoded.id());
        }
        String sql = "SELECT g.id, g.name, g.code, g.created_at, (" + JOINED + ") AS member_count, "
            + "(SELECT COUNT(*) " + GROUP_RECORDS + " AND r.uploaded_at >= :week) AS records_week, "
            + "(SELECT MAX(r.uploaded_at) " + GROUP_RECORDS + ") AS last_record_at "
            + "FROM mody_group g" + where + " ORDER BY g.created_at DESC, g.id DESC LIMIT :limit";
        List<Item> rows = jdbc.query(sql, params, (rs, n) -> new Item(Long.toString(rs.getLong("id")),
            rs.getString("name"), rs.getString("code"), time(rs, "created_at"), rs.getLong("member_count"),
            rs.getLong("records_week"), time(rs, "last_record_at")));
        boolean hasMore = rows.size() > size;
        List<Item> items = hasMore ? rows.subList(0, size) : rows;
        return new Page(items, hasMore ? encode(items.getLast()) : null, hasMore);
    }

    public Detail detail(long groupId) {
        var id = new MapSqlParameterSource("id", groupId)
            .addValue("week", LocalDate.now(KST).minusDays(6).atStartOfDay());
        List<Detail> found = jdbc.query("SELECT g.id, g.name, g.code, g.created_at, g.status, g.deleted_at, "
            + "(SELECT COUNT(*) " + GROUP_RECORDS + ") AS records, "
            + "(SELECT COUNT(*) " + GROUP_RECORDS + " AND r.uploaded_at >= :week) AS records_week, "
            + "(SELECT COUNT(*) FROM record_comment c WHERE c.group_id = g.id AND c.status = 'ACTIVE' "
            + "AND c.deleted_at IS NULL) AS comments "
            + "FROM mody_group g WHERE g.id = :id", id, (rs, n) -> new Detail(Long.toString(rs.getLong("id")),
            rs.getString("name"), rs.getString("code"), time(rs, "created_at"),
            "ACTIVE".equals(rs.getString("status")) && rs.getTimestamp("deleted_at") == null,
            rs.getLong("records"), rs.getLong("records_week"), rs.getLong("comments"),
            List.of(), List.of(), List.of()));
        if (found.isEmpty()) {
            throw new GeneralException(ErrorStatus.NOT_FOUND);
        }
        Detail base = found.getFirst();
        List<Member> members = jdbc.query("""
            SELECT gm.member_id, gm.display_nickname, gm.group_member_status, gm.joined_at, gm.left_at,
                m.nickname, m.birth_date, m.status AS member_status, m.deleted_at AS member_deleted_at
            FROM group_member gm LEFT JOIN member m ON m.id = gm.member_id
            WHERE gm.group_id = :id AND gm.status = 'ACTIVE' AND gm.deleted_at IS NULL
            ORDER BY CASE WHEN gm.group_member_status = 'JOINED' THEN 0 ELSE 1 END, gm.joined_at
            LIMIT 200
            """, id, (rs, n) -> {
            boolean active = "ACTIVE".equals(rs.getString("member_status"))
                && rs.getTimestamp("member_deleted_at") == null;
            return new Member(Long.toString(rs.getLong("member_id")),
                active ? rs.getString("nickname") : WITHDRAWN, rs.getString("display_nickname"),
                rs.getDate("birth_date") == null ? null : rs.getDate("birth_date").toLocalDate(),
                rs.getString("group_member_status"), time(rs, "joined_at"), time(rs, "left_at"), active);
        });
        LocalDate today = LocalDate.now(KST);
        List<Challenge> challenges = jdbc.query("""
            SELECT gc.id, gc.global_weekly_challenge_id, gc.starts_on, gc.ends_on, gc.group_challenge_status,
                c.title, c.challenge_type,
                (SELECT COUNT(*) FROM challenge_proof p WHERE p.group_challenge_id = gc.id
                    AND p.status = 'ACTIVE' AND p.deleted_at IS NULL) AS proofs
            FROM group_challenge gc LEFT JOIN challenge c ON c.id = gc.challenge_id
            WHERE gc.group_id = :id AND gc.status = 'ACTIVE' AND gc.deleted_at IS NULL
            ORDER BY gc.starts_on DESC, gc.id DESC
            LIMIT 30
            """, id, (rs, n) -> {
            LocalDate starts = rs.getDate("starts_on").toLocalDate();
            LocalDate ends = rs.getDate("ends_on").toLocalDate();
            String period = today.isBefore(starts) ? "UPCOMING" : today.isAfter(ends) ? "PAST" : "CURRENT";
            Object global = rs.getObject("global_weekly_challenge_id");
            return new Challenge(Long.toString(rs.getLong("id")), global == null ? null : global.toString(),
                rs.getString("title"), rs.getString("challenge_type"), starts, ends,
                rs.getString("group_challenge_status"), period, rs.getLong("proofs"));
        });
        List<Comment> comments = jdbc.query("""
            SELECT c.id, c.record_id, c.member_id, c.content, c.emoji_id, c.created_at,
                CASE WHEN m.status = 'ACTIVE' AND m.deleted_at IS NULL THEN m.nickname ELSE '탈퇴한 회원' END
                    AS nickname,
                r.record_type, r.menu, r.exercise_name
            FROM record_comment c
            LEFT JOIN member m ON m.id = c.member_id
            LEFT JOIN activity_record r ON r.id = c.record_id
            WHERE c.group_id = :id AND c.status = 'ACTIVE' AND c.deleted_at IS NULL
            ORDER BY c.created_at DESC, c.id DESC
            LIMIT 50
            """, id, (rs, n) -> {
            String type = rs.getString("record_type");
            return new Comment(Long.toString(rs.getLong("id")), Long.toString(rs.getLong("record_id")), type,
                "MEAL".equals(type) ? rs.getString("menu") : rs.getString("exercise_name"),
                Long.toString(rs.getLong("member_id")), rs.getString("nickname"), rs.getString("content"),
                rs.getString("emoji_id"), time(rs, "created_at"));
        });
        return new Detail(base.id(), base.name(), base.code(), base.createdAt(), base.active(), base.recordCount(),
            base.recordsLast7Days(), base.commentCount(), members, challenges, comments);
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static String encode(Item item) {
        String raw = item.createdAt() + "|" + item.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decode(String cursor) {
        try {
            if (cursor.length() > 256) {
                throw new IllegalArgumentException();
            }
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            return new Cursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException exception) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
    }

    private record Cursor(LocalDateTime createdAt, long id) { }

    public record Item(String id, String name, String code, LocalDateTime createdAt, long memberCount,
        long recordsLast7Days, LocalDateTime lastRecordAt) { }

    public record Page(List<Item> items, String nextCursor, boolean hasMore) { }

    /** nickname은 계정 닉네임(탈퇴 시 '탈퇴한 회원'), displayNickname은 이 그룹에서 보이는 닉네임이다. */
    public record Member(String memberId, String nickname, String displayNickname, LocalDate birthDate,
        String memberStatus, LocalDateTime joinedAt, LocalDateTime leftAt, boolean accountActive) { }

    /** period: CURRENT(오늘 포함), PAST, UPCOMING. 한국시간 기준. */
    public record Challenge(String id, String globalWeeklyChallengeId, String title, String challengeType,
        LocalDate startsOn, LocalDate endsOn, String status, String period, long proofCount) { }

    public record Comment(String id, String recordId, String recordType, String recordTitle, String memberId,
        String nickname, String content, String emojiId, LocalDateTime createdAt) { }

    public record Detail(String id, String name, String code, LocalDateTime createdAt, boolean active,
        long recordCount, long recordsLast7Days, long commentCount, List<Member> members,
        List<Challenge> challenges, List<Comment> recentComments) { }
}
