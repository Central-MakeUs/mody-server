package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.upload.ImageUrlResolver;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFeedService {
    private static final Set<String> TYPES = Set.of("MEAL", "EXERCISE", "CHALLENGE");
    private static final String ACTIVITY = """
        SELECT r.id, CONCAT('', r.record_type) AS feed_type, r.uploaded_at, r.member_id,
            CASE WHEN m.status = 'ACTIVE' AND m.deleted_at IS NULL THEN m.nickname ELSE '탈퇴한 회원' END AS nickname,
            r.image_key, r.crop_x, r.crop_y, r.crop_width, r.crop_height,
            CASE WHEN r.record_type = 'MEAL' THEN r.menu ELSE r.exercise_name END AS title,
            NULL AS description, r.meal_time, r.exercise_duration_minutes,
            NULL AS group_challenge_id, NULL AS global_weekly_challenge_id, r.group_id AS legacy_group_id
        FROM activity_record r LEFT JOIN member m ON m.id = r.member_id
        WHERE r.status = 'ACTIVE' AND r.deleted_at IS NULL
        """;
    private static final String PROOF = """
        SELECT r.id, 'CHALLENGE' AS feed_type, r.uploaded_at, r.member_id,
            CASE WHEN m.status = 'ACTIVE' AND m.deleted_at IS NULL THEN m.nickname ELSE '탈퇴한 회원' END AS nickname,
            r.image_key, r.crop_x, r.crop_y, r.crop_width, r.crop_height,
            c.title, c.description, NULL AS meal_time, NULL AS exercise_duration_minutes,
            gc.id AS group_challenge_id, gc.global_weekly_challenge_id, gc.group_id AS legacy_group_id
        FROM challenge_proof r
        JOIN group_challenge gc ON gc.id = r.group_challenge_id AND gc.status = 'ACTIVE' AND gc.deleted_at IS NULL
        JOIN challenge c ON c.id = gc.challenge_id AND c.status = 'ACTIVE' AND c.deleted_at IS NULL
        LEFT JOIN member m ON m.id = r.member_id
        WHERE r.status = 'ACTIVE' AND r.deleted_at IS NULL
        """;
    private static final String ACTIVE_MAPPING = """
        SELECT 1 FROM activity_record_group arg
        WHERE arg.record_id = r.id AND arg.status = 'ACTIVE' AND arg.deleted_at IS NULL
        """;
    private final NamedParameterJdbcTemplate jdbc;
    private final ImageUrlResolver images;

    public Page list(LocalDate from, LocalDate to, String type, Long memberId, Long groupId,
        Long groupChallengeId, String keyword, String cursor, int size) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        from = from == null ? today.minusDays(6) : from;
        to = to == null ? today : to;
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 365 || size < 1 || size > 100
            || (type != null && !TYPES.contains(type)) || (memberId != null && memberId <= 0)
            || (groupId != null && groupId <= 0) || (groupChallengeId != null && groupChallengeId <= 0)
            || (keyword != null && keyword.length() > 100) || to.equals(LocalDate.MAX)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        var params = new MapSqlParameterSource().addValue("from", from.atStartOfDay())
            .addValue("until", to.plusDays(1).atStartOfDay()).addValue("limit", size + 1);
        String common = " AND r.uploaded_at >= :from AND r.uploaded_at < :until";
        if (memberId != null) {
            params.addValue("member", memberId);
            common += " AND r.member_id = :member";
        }
        Cursor after = decode(cursor);
        String activity = ACTIVITY + common, proof = PROOF + common;
        if (type != null) {
            params.addValue("type", type);
            if (type.equals("CHALLENGE")) {
                activity += " AND 1 = 0";
            } else {
                activity += " AND r.record_type = :type";
                proof += " AND 1 = 0";
            }
        }
        if (groupId != null) {
            params.addValue("group", groupId);
            activity += " AND (EXISTS (" + ACTIVE_MAPPING + " AND arg.group_id = :group)"
                + " OR (r.group_id = :group AND NOT EXISTS (" + ACTIVE_MAPPING + ")))";
            proof += " AND gc.group_id = :group";
        }
        if (groupChallengeId != null) {
            params.addValue("challenge", groupChallengeId);
            activity += " AND 1 = 0";
            proof += " AND gc.id = :challenge";
        }
        if (keyword != null && !keyword.isBlank()) {
            params.addValue("search", "%" + keyword.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%");
            // The same visible author label is searched; deleted members' old nicknames are not exposed.
            String nickname = "CASE WHEN m.status = 'ACTIVE' AND m.deleted_at IS NULL"
                + " THEN m.nickname ELSE '탈퇴한 회원' END";
            activity += " AND (" + nickname + " LIKE :search ESCAPE '!' OR r.menu LIKE :search ESCAPE '!'"
                + " OR r.exercise_name LIKE :search ESCAPE '!')";
            proof += " AND (" + nickname + " LIKE :search ESCAPE '!' OR c.title LIKE :search ESCAPE '!')";
        }
        if (after != null) {
            params.addValue("at", after.at()).addValue("afterType", after.type()).addValue("afterId", after.id());
            activity += cursorCondition("CONCAT('', r.record_type)");
            proof += cursorCondition("'CHALLENGE'");
        }
        // Bound each source before UNION: one group-shared activity remains one feed item.
        String order = " ORDER BY uploaded_at DESC, feed_type DESC, id DESC LIMIT :limit";
        String sql = "SELECT * FROM (SELECT * FROM (" + activity + order + ") a UNION ALL "
            + "SELECT * FROM (" + proof + order + ") p) feed" + order;
        List<Item> rows = jdbc.query(sql, params, (rs, n) -> item(rs));
        boolean hasMore = rows.size() > size;
        List<Item> items = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? encode(items.get(items.size() - 1)) : null;
        return new Page(items, next, hasMore, from, to, "Asia/Seoul");
    }

    public Detail detail(String type, long id) {
        if (!TYPES.contains(type) || id <= 0) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        var params = new MapSqlParameterSource("id", id).addValue("type", type);
        String sql = type.equals("CHALLENGE") ? PROOF + " AND r.id = :id"
            : ACTIVITY + " AND r.id = :id AND r.record_type = :type";
        List<Item> found = jdbc.query(sql, params, (rs, n) -> item(rs));
        if (found.isEmpty()) {
            throw new GeneralException(ErrorStatus.NOT_FOUND);
        }
        Item item = found.getFirst();
        String groupsQuery;
        if (type.equals("CHALLENGE")) {
            groupsQuery = "SELECT id, name FROM mody_group WHERE id = :group";
        } else {
            groupsQuery = """
                SELECT DISTINCT g.id, g.name FROM mody_group g
                WHERE (EXISTS (SELECT 1 FROM activity_record_group arg WHERE arg.record_id = :id
                    AND arg.group_id = g.id AND arg.status = 'ACTIVE' AND arg.deleted_at IS NULL)
                    OR (g.id = :group AND NOT EXISTS (SELECT 1 FROM activity_record_group arg
                        WHERE arg.record_id = :id AND arg.status = 'ACTIVE' AND arg.deleted_at IS NULL)))
                """;
        }
        params.addValue("group", item.legacyGroupId() == null ? null : Long.valueOf(item.legacyGroupId()));
        List<Group> groups = jdbc.query(groupsQuery + " AND status = 'ACTIVE' AND deleted_at IS NULL ORDER BY id",
            params, (rs, n) -> new Group(Long.toString(rs.getLong("id")), rs.getString("name")));
        return new Detail(item, groups);
    }

    private static String cursorCondition(String typeExpression) {
        return " AND (r.uploaded_at < :at OR (r.uploaded_at = :at AND (" + typeExpression
            + " < :afterType OR (" + typeExpression + " = :afterType AND r.id < :afterId))))";
    }

    private Item item(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Item(Long.toString(rs.getLong("id")), rs.getString("feed_type"),
            rs.getTimestamp("uploaded_at").toLocalDateTime(), Long.toString(rs.getLong("member_id")),
            rs.getString("nickname"), images.resolve(rs.getString("image_key")), rs.getString("title"),
            rs.getString("description"), rs.getString("meal_time"),
            (Integer) rs.getObject("exercise_duration_minutes"), stringId(rs.getObject("group_challenge_id")),
            stringId(rs.getObject("global_weekly_challenge_id")), stringId(rs.getObject("legacy_group_id")),
            new Crop(rs.getBigDecimal("crop_x"), rs.getBigDecimal("crop_y"),
                rs.getBigDecimal("crop_width"), rs.getBigDecimal("crop_height")));
    }

    private static String stringId(Object value) {
        return value == null ? null : value.toString();
    }

    private static Cursor decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            if (encoded.length() > 256) {
                throw new IllegalArgumentException();
            }
            String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length != 4 || !parts[0].equals("1") || !TYPES.contains(parts[2])) {
                throw new IllegalArgumentException();
            }
            long id = Long.parseLong(parts[3]);
            if (id <= 0) {
                throw new IllegalArgumentException();
            }
            return new Cursor(LocalDateTime.parse(parts[1]), parts[2], id);
        } catch (RuntimeException exception) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
    }

    private static String encode(Item item) {
        String value = "1|" + item.uploadedAt() + "|" + item.type() + "|" + item.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record Cursor(LocalDateTime at, String type, long id) { }
    public record Crop(BigDecimal x, BigDecimal y, BigDecimal width, BigDecimal height) { }
    public record Group(String id, String name) { }
    public record Item(String id, String type, LocalDateTime uploadedAt, String memberId, String nickname,
        String imageUrl, String title, String description, String mealTime, Integer exerciseDurationMinutes,
        String groupChallengeId, String globalWeeklyChallengeId, String legacyGroupId, Crop crop) { }
    public record Page(List<Item> items, String nextCursor, boolean hasMore, LocalDate from, LocalDate to,
        String timezone) { }
    public record Detail(Item item, List<Group> groups) { }
}
