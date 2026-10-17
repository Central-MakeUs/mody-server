package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.upload.ImageUrlResolver;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
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
 * 관리 화면의 회원 검색과 상세 조회. 토큰 값, 소셜 식별자 같은 비밀 정보는 내보내지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Set<String> STATES = Set.of("ALL", "ACTIVE", "WITHDRAWN");
    private static final Set<String> LOGIN_TYPES = Set.of("KAKAO", "APPLE", "GOOGLE", "ANDROIDTEST", "IOSTEST");
    private static final String ACTIVE = "m.status = 'ACTIVE' AND m.deleted_at IS NULL";
    private static final String LIVE_RECORD = "r.member_id = m.id AND r.status = 'ACTIVE' AND r.deleted_at IS NULL";
    private final NamedParameterJdbcTemplate jdbc;
    private final ImageUrlResolver images;

    public Page list(String keyword, String state, String loginType, String cursor, int size) {
        if (size < 1 || size > 100) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        var params = new MapSqlParameterSource().addValue("limit", size + 1);
        StringBuilder where = filters(keyword, state, loginType, params);
        if (StringUtils.hasText(cursor)) {
            Cursor decoded = decode(cursor);
            where.append(" AND (m.created_at < :cursorAt OR (m.created_at = :cursorAt AND m.id < :cursorId))");
            params.addValue("cursorAt", decoded.createdAt()).addValue("cursorId", decoded.id());
        }
        String sql = """
            SELECT m.id, m.nickname, m.profile_image_key, m.created_at, m.deleted_at, m.status,
                (SELECT COUNT(*) FROM group_member gm WHERE gm.member_id = m.id
                    AND gm.group_member_status = 'JOINED' AND gm.status = 'ACTIVE' AND gm.deleted_at IS NULL)
                    AS group_count,
                (SELECT COUNT(*) FROM activity_record r WHERE %1$s) AS record_count,
                (SELECT MAX(r.uploaded_at) FROM activity_record r WHERE %1$s) AS last_record_at
            FROM member m
            """.formatted(LIVE_RECORD) + where + " ORDER BY m.created_at DESC, m.id DESC LIMIT :limit";
        List<Item> rows = jdbc.query(sql, params, (rs, n) -> new Item(
            Long.toString(rs.getLong("id")), displayName(rs), images.resolve(rs.getString("profile_image_key")),
            List.of(), time(rs, "created_at"), time(rs, "deleted_at"), active(rs),
            rs.getLong("group_count"), rs.getLong("record_count"), time(rs, "last_record_at")));
        boolean hasMore = rows.size() > size;
        List<Item> items = withLoginTypes(hasMore ? rows.subList(0, size) : rows);
        String next = hasMore ? encode(items.getLast()) : null;
        return new Page(items, next, hasMore);
    }

    public Summary summary() {
        LocalDateTime today = LocalDate.now(KST).atStartOfDay();
        var params = new MapSqlParameterSource()
            .addValue("today", today).addValue("week", today.minusDays(6));
        return jdbc.queryForObject("""
            SELECT COUNT(*) AS total,
                SUM(CASE WHEN m.status = 'ACTIVE' AND m.deleted_at IS NULL THEN 1 ELSE 0 END) AS active,
                SUM(CASE WHEN m.created_at >= :today THEN 1 ELSE 0 END) AS joined_today,
                SUM(CASE WHEN m.created_at >= :week THEN 1 ELSE 0 END) AS joined_week,
                SUM(CASE WHEN m.deleted_at >= :week THEN 1 ELSE 0 END) AS withdrawn_week
            FROM member m
            """, params, (rs, n) -> {
            long total = rs.getLong("total"), active = rs.getLong("active");
            return new Summary(total, active, total - active, rs.getLong("joined_today"),
                rs.getLong("joined_week"), rs.getLong("withdrawn_week"));
        });
    }

    public Detail detail(long memberId) {
        var id = new MapSqlParameterSource("id", memberId);
        List<Detail> found = jdbc.query("""
            SELECT m.id, m.nickname, m.profile_image_key, m.birth_date, m.target_weight_kg,
                m.health_connection_status, m.group_onboarding_completed, m.created_at, m.deleted_at, m.status
            FROM member m WHERE m.id = :id
            """, id, (rs, n) -> new Detail(Long.toString(rs.getLong("id")), displayName(rs),
            images.resolve(rs.getString("profile_image_key")),
            rs.getDate("birth_date") == null ? null : rs.getDate("birth_date").toLocalDate(),
            rs.getObject("target_weight_kg", BigDecimal.class), rs.getString("health_connection_status"),
            rs.getBoolean("group_onboarding_completed"), time(rs, "created_at"), time(rs, "deleted_at"),
            active(rs), List.of(), List.of(), null, List.of(), List.of(), 0));
        if (found.isEmpty()) {
            throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
        }
        Detail base = found.getFirst();
        List<Login> logins = jdbc.query("""
            SELECT login_type, created_at, status, deleted_at FROM social_account
            WHERE member_id = :id ORDER BY created_at
            """, id, (rs, n) -> new Login(rs.getString("login_type"), time(rs, "created_at"),
            "ACTIVE".equals(rs.getString("status")) && rs.getTimestamp("deleted_at") == null));
        List<Group> groups = jdbc.query("""
            SELECT g.id, g.name, gm.group_member_status, gm.display_nickname, gm.joined_at, gm.left_at,
                g.status AS group_status, g.deleted_at AS group_deleted_at
            FROM group_member gm JOIN mody_group g ON g.id = gm.group_id
            WHERE gm.member_id = :id
            ORDER BY gm.joined_at DESC LIMIT 50
            """, id, (rs, n) -> new Group(Long.toString(rs.getLong("id")), rs.getString("name"),
            rs.getString("group_member_status"), rs.getString("display_nickname"), time(rs, "joined_at"),
            time(rs, "left_at"),
            "ACTIVE".equals(rs.getString("group_status")) && rs.getTimestamp("group_deleted_at") == null));
        Counts counts = jdbc.queryForObject("""
            SELECT
                (SELECT COUNT(*) FROM activity_record r WHERE r.member_id = :id AND r.record_type = 'MEAL'
                    AND r.status = 'ACTIVE' AND r.deleted_at IS NULL) AS meals,
                (SELECT COUNT(*) FROM activity_record r WHERE r.member_id = :id AND r.record_type = 'EXERCISE'
                    AND r.status = 'ACTIVE' AND r.deleted_at IS NULL) AS exercises,
                (SELECT COUNT(*) FROM challenge_proof p WHERE p.member_id = :id
                    AND p.status = 'ACTIVE' AND p.deleted_at IS NULL) AS proofs,
                (SELECT COUNT(*) FROM record_comment c WHERE c.member_id = :id
                    AND c.status = 'ACTIVE' AND c.deleted_at IS NULL) AS comments,
                (SELECT COUNT(*) FROM step_record s WHERE s.member_id = :id
                    AND s.status = 'ACTIVE' AND s.deleted_at IS NULL) AS step_days,
                (SELECT COALESCE(SUM(s.step_count), 0) FROM step_record s WHERE s.member_id = :id
                    AND s.status = 'ACTIVE' AND s.deleted_at IS NULL) AS steps,
                (SELECT COUNT(*) FROM record_report rr WHERE rr.reporter_member_id = :id
                    AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL) AS reports_made,
                (SELECT COUNT(*) FROM record_report rr JOIN activity_record r ON r.id = rr.record_id
                    WHERE r.member_id = :id AND rr.status = 'ACTIVE' AND rr.deleted_at IS NULL) AS reports_received,
                (SELECT MAX(r.uploaded_at) FROM activity_record r WHERE r.member_id = :id
                    AND r.status = 'ACTIVE' AND r.deleted_at IS NULL) AS last_record_at
            FROM member WHERE id = :id
            """, id, (rs, n) -> new Counts(rs.getLong("meals"), rs.getLong("exercises"), rs.getLong("proofs"),
            rs.getLong("comments"), rs.getLong("step_days"), rs.getLong("steps"), rs.getLong("reports_made"),
            rs.getLong("reports_received"), time(rs, "last_record_at")));
        List<Device> devices = jdbc.query("""
            SELECT platform, enabled, last_seen_at FROM member_push_token
            WHERE member_id = :id AND status = 'ACTIVE' AND deleted_at IS NULL
            ORDER BY last_seen_at DESC
            """, id, (rs, n) -> new Device(rs.getString("platform"), rs.getBoolean("enabled"),
            time(rs, "last_seen_at")));
        List<Record> records = jdbc.query("""
            SELECT r.id, r.record_type, r.menu, r.exercise_name, r.image_key, r.uploaded_at
            FROM activity_record r WHERE r.member_id = :id AND r.status = 'ACTIVE' AND r.deleted_at IS NULL
            ORDER BY r.uploaded_at DESC, r.id DESC LIMIT 12
            """, id, (rs, n) -> new Record(Long.toString(rs.getLong("id")), rs.getString("record_type"),
            "MEAL".equals(rs.getString("record_type")) ? rs.getString("menu") : rs.getString("exercise_name"),
            images.resolve(rs.getString("image_key")), time(rs, "uploaded_at")));
        Integer sessions = jdbc.queryForObject("""
            SELECT COUNT(*) FROM refresh_token WHERE member_id = :id AND status = 'ACTIVE' AND deleted_at IS NULL
            """, id, Integer.class);
        return new Detail(base.id(), base.nickname(), base.profileImageUrl(), base.birthDate(),
            base.targetWeightKg(), base.healthConnection(), base.groupOnboardingCompleted(), base.createdAt(),
            base.deletedAt(), base.active(), logins, groups, counts, devices, records,
            sessions == null ? 0 : sessions);
    }

    private StringBuilder filters(String keyword, String state, String loginType, MapSqlParameterSource params) {
        String normalizedState = StringUtils.hasText(state) ? state.trim().toUpperCase(Locale.ROOT) : "ALL";
        if (!STATES.contains(normalizedState)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if ("ACTIVE".equals(normalizedState)) {
            where.append(" AND ").append(ACTIVE);
        } else if ("WITHDRAWN".equals(normalizedState)) {
            where.append(" AND NOT (").append(ACTIVE).append(")");
        }
        if (StringUtils.hasText(loginType)) {
            String normalized = loginType.trim().toUpperCase(Locale.ROOT);
            if (!LOGIN_TYPES.contains(normalized)) {
                throw new GeneralException(ErrorStatus.BAD_REQUEST);
            }
            where.append(" AND EXISTS (SELECT 1 FROM social_account sa WHERE sa.member_id = m.id"
                + " AND sa.login_type = :loginType)");
            params.addValue("loginType", normalized);
        }
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            if (trimmed.length() > 100) {
                throw new GeneralException(ErrorStatus.BAD_REQUEST);
            }
            params.addValue("search", "%" + trimmed.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%");
            if (trimmed.chars().allMatch(Character::isDigit) && trimmed.length() <= 18) {
                where.append(" AND (m.id = :memberId OR m.nickname LIKE :search ESCAPE '!')");
                params.addValue("memberId", Long.parseLong(trimmed));
            } else {
                where.append(" AND m.nickname LIKE :search ESCAPE '!'");
            }
        }
        return where;
    }

    private List<Item> withLoginTypes(List<Item> items) {
        if (items.isEmpty()) {
            return items;
        }
        Map<String, List<String>> types = new LinkedHashMap<>();
        jdbc.query("""
            SELECT member_id, login_type FROM social_account WHERE member_id IN (:ids) ORDER BY created_at
            """, new MapSqlParameterSource("ids", items.stream().map(item -> Long.parseLong(item.id())).toList()),
            rs -> {
                types.computeIfAbsent(Long.toString(rs.getLong("member_id")), key -> new java.util.ArrayList<>())
                    .add(rs.getString("login_type"));
            });
        return items.stream().map(item -> new Item(item.id(), item.nickname(), item.profileImageUrl(),
            types.getOrDefault(item.id(), List.of()).stream().distinct().toList(), item.createdAt(),
            item.deletedAt(), item.active(), item.groupCount(), item.recordCount(), item.lastRecordAt())).toList();
    }

    private static String displayName(ResultSet rs) throws SQLException {
        return rs.getString("nickname");
    }

    private static boolean active(ResultSet rs) throws SQLException {
        return "ACTIVE".equals(rs.getString("status")) && rs.getTimestamp("deleted_at") == null;
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String encode(Item item) {
        String raw = item.createdAt() + "|" + item.id();
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

    private record Cursor(LocalDateTime createdAt, long id) { }

    public record Item(String id, String nickname, String profileImageUrl, List<String> loginTypes,
        LocalDateTime createdAt, LocalDateTime deletedAt, boolean active, long groupCount, long recordCount,
        LocalDateTime lastRecordAt) { }

    public record Page(List<Item> items, String nextCursor, boolean hasMore) { }

    public record Summary(long totalCount, long activeCount, long withdrawnCount, long joinedToday,
        long joinedLast7Days, long withdrawnLast7Days) { }

    public record Login(String loginType, LocalDateTime linkedAt, boolean active) { }

    public record Group(String id, String name, String memberStatus, String displayNickname,
        LocalDateTime joinedAt, LocalDateTime leftAt, boolean groupActive) { }

    public record Counts(long mealRecords, long exerciseRecords, long challengeProofs, long comments,
        long stepDays, long totalSteps, long reportsMade, long reportsReceived, LocalDateTime lastRecordAt) { }

    public record Device(String platform, boolean enabled, LocalDateTime lastSeenAt) { }

    public record Record(String id, String type, String title, String imageUrl, LocalDateTime uploadedAt) { }

    public record Detail(String id, String nickname, String profileImageUrl, LocalDate birthDate,
        BigDecimal targetWeightKg, String healthConnection, boolean groupOnboardingCompleted,
        LocalDateTime createdAt, LocalDateTime deletedAt, boolean active, List<Login> logins, List<Group> groups,
        Counts counts, List<Device> devices, List<Record> recentRecords, int activeSessions) { }
}
