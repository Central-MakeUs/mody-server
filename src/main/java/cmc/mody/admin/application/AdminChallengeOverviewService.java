package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminChallengeOverviewService {
    private static final Set<String> PHASES = Set.of("ALL", "ACTIVE", "UPCOMING", "ENDED");
    private static final String GLOBAL = """
        SELECT gw.id, 'GLOBAL' AS kind, c.title, c.description, gw.starts_on, gw.ends_on,
            gw.id AS global_id, NULL AS group_id, NULL AS group_name
        FROM global_weekly_challenge gw
        JOIN challenge c ON c.id = gw.challenge_id AND c.status = 'ACTIVE' AND c.deleted_at IS NULL
        WHERE gw.status = 'ACTIVE' AND gw.deleted_at IS NULL
        """;
    private static final String GROUP = """
        SELECT gc.id, 'GROUP' AS kind, c.title, c.description, gc.starts_on, gc.ends_on,
            gc.global_weekly_challenge_id AS global_id, gc.group_id, g.name AS group_name
        FROM group_challenge gc
        JOIN challenge c ON c.id = gc.challenge_id AND c.status = 'ACTIVE' AND c.deleted_at IS NULL
        JOIN mody_group g ON g.id = gc.group_id AND g.status = 'ACTIVE' AND g.deleted_at IS NULL
        WHERE gc.status = 'ACTIVE' AND gc.deleted_at IS NULL
        """;
    private static final String STATS = """
        SELECT %s AS stats_id, '%s' AS kind,
            COUNT(DISTINCT gc.group_id) AS linked_groups, COUNT(p.id) AS proofs,
            COUNT(DISTINCT p.member_id) AS participants,
            COUNT(DISTINCT CASE WHEN p.id IS NOT NULL THEN gc.group_id END) AS participating_groups
        FROM group_challenge gc
        JOIN challenge c ON c.id = gc.challenge_id AND c.status = 'ACTIVE' AND c.deleted_at IS NULL
        JOIN mody_group g ON g.id = gc.group_id AND g.status = 'ACTIVE' AND g.deleted_at IS NULL
        LEFT JOIN challenge_proof p ON p.group_challenge_id = gc.id
            AND p.status = 'ACTIVE' AND p.deleted_at IS NULL
            AND p.uploaded_at >= gc.starts_on AND p.uploaded_at < TIMESTAMPADD(DAY, 1, gc.ends_on)
        WHERE gc.status = 'ACTIVE' AND gc.deleted_at IS NULL AND %s IN (:%s)
        GROUP BY %s
        """;
    private final NamedParameterJdbcTemplate jdbc;

    public Page list(LocalDate from, LocalDate to, Long groupId, String phase, String keyword,
        String cursor, int size) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        from = from == null ? today.minusDays(90) : from;
        to = to == null ? today.plusDays(30) : to;
        phase = phase == null ? "ALL" : phase;
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 365 || size < 1 || size > 100
            || !PHASES.contains(phase) || (groupId != null && groupId <= 0)
            || (keyword != null && keyword.length() > 100)) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        if (groupId != null && jdbc.queryForObject(
            "SELECT COUNT(*) FROM mody_group WHERE id = :id AND status = 'ACTIVE' AND deleted_at IS NULL",
            new MapSqlParameterSource("id", groupId), Long.class) == 0) {
            throw new GeneralException(ErrorStatus.GROUP_NOT_FOUND);
        }
        var params = new MapSqlParameterSource().addValue("from", from).addValue("to", to)
            .addValue("today", today).addValue("limit", size + 1);
        String common = " AND starts_on <= :to AND ends_on >= :from";
        String global = GLOBAL + common, group = GROUP + common;
        if (groupId == null) {
            group += " AND gc.global_weekly_challenge_id IS NULL";
        } else {
            params.addValue("group", groupId);
            global += " AND 1 = 0";
            group += " AND gc.group_id = :group";
        }
        if (keyword != null && !keyword.isBlank()) {
            params.addValue("search", "%" + keyword.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%");
            global += " AND (c.title LIKE :search ESCAPE '!' OR c.description LIKE :search ESCAPE '!')";
            group += " AND (c.title LIKE :search ESCAPE '!' OR c.description LIKE :search ESCAPE '!')";
        }
        String base = "SELECT * FROM (" + global + " UNION ALL " + group + ") weekly";
        Summary summary = jdbc.queryForObject("SELECT COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN starts_on <= :today AND ends_on >= :today THEN 1 ELSE 0 END), 0) AS active, "
            + "COALESCE(SUM(CASE WHEN starts_on > :today THEN 1 ELSE 0 END), 0) AS upcoming, "
            + "COALESCE(SUM(CASE WHEN ends_on < :today THEN 1 ELSE 0 END), 0) AS ended FROM ("
            + base + ") counts", params, (rs, n) -> new Summary(rs.getLong("total"), rs.getLong("active"),
                rs.getLong("upcoming"), rs.getLong("ended")));
        String condition = switch (phase) {
            case "ACTIVE" -> "starts_on <= :today AND ends_on >= :today";
            case "UPCOMING" -> "starts_on > :today";
            case "ENDED" -> "ends_on < :today";
            default -> "1 = 1";
        };
        long total = switch (phase) {
            case "ACTIVE" -> summary.activeCount();
            case "UPCOMING" -> summary.upcomingCount();
            case "ENDED" -> summary.endedCount();
            default -> summary.totalCount();
        };
        Cursor after = decode(cursor);
        if (after != null) {
            params.addValue("at", after.at()).addValue("kind", after.kind()).addValue("id", after.id());
            condition += " AND (starts_on < :at OR (starts_on = :at AND (kind < :kind"
                + " OR (kind = :kind AND id < :id))))";
        }
        List<Definition> rows = jdbc.query(base + " WHERE " + condition
            + " ORDER BY starts_on DESC, kind DESC, id DESC LIMIT :limit", params,
            (rs, n) -> new Definition(Long.toString(rs.getLong("id")), rs.getString("kind"),
                rs.getString("title"), rs.getString("description"), rs.getDate("starts_on").toLocalDate(),
                rs.getDate("ends_on").toLocalDate(), stringId(rs.getObject("global_id")),
                stringId(rs.getObject("group_id")), rs.getString("group_name")));
        boolean hasMore = rows.size() > size;
        List<Definition> definitions = hasMore ? rows.subList(0, size) : rows;
        Map<String, Counts> counts = statistics(definitions);
        List<Item> items = definitions.stream().map(row -> {
            Counts count = counts.getOrDefault(row.kind() + ":" + row.id(), new Counts(0, 0, 0, 0));
            String state = row.startsOn().isAfter(today) ? "UPCOMING"
                : row.endsOn().isBefore(today) ? "ENDED" : "ACTIVE";
            return new Item(row.id(), row.kind(), row.title(), row.description(), row.startsOn(), row.endsOn(),
                row.globalId(), row.groupId(), row.groupName(), state, count.linked(), count.proofs(),
                count.participants(), count.participatingGroups());
        }).toList();
        return new Page(items, total, summary, hasMore,
            hasMore ? encode(definitions.getLast()) : null, from, to, "Asia/Seoul");
    }

    private Map<String, Counts> statistics(List<Definition> definitions) {
        if (definitions.isEmpty()) {
            return Map.of();
        }
        List<Long> globals = ids(definitions, "GLOBAL"), groups = ids(definitions, "GROUP");
        var params = new MapSqlParameterSource("globalIds", globals).addValue("groupIds", groups);
        String global = STATS.formatted("gc.global_weekly_challenge_id", "GLOBAL",
            "gc.global_weekly_challenge_id", "globalIds", "gc.global_weekly_challenge_id");
        String group = STATS.formatted("gc.id", "GROUP", "gc.id", "groupIds", "gc.id");
        Map<String, Counts> counts = new HashMap<>();
        jdbc.query(global + " UNION ALL " + group, params, rs -> {
            counts.put(rs.getString("kind") + ":" + rs.getLong("stats_id"),
                new Counts(rs.getLong("linked_groups"), rs.getLong("proofs"),
                    rs.getLong("participants"), rs.getLong("participating_groups")));
        });
        return counts;
    }

    private static List<Long> ids(List<Definition> rows, String kind) {
        List<Long> ids = rows.stream().filter(row -> row.kind().equals(kind))
            .map(row -> Long.valueOf(row.id())).toList();
        return ids.isEmpty() ? List.of(-1L) : ids;
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
            String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\\|");
            if (parts.length != 4 || !parts[0].equals("1") || !Set.of("GLOBAL", "GROUP").contains(parts[2])) {
                throw new IllegalArgumentException();
            }
            long id = Long.parseLong(parts[3]);
            if (id <= 0) {
                throw new IllegalArgumentException();
            }
            return new Cursor(LocalDate.parse(parts[1]), parts[2], id);
        } catch (RuntimeException exception) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
    }

    private static String encode(Definition row) {
        String value = "1|" + row.startsOn() + "|" + row.kind() + "|" + row.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record Cursor(LocalDate at, String kind, long id) { }
    private record Definition(String id, String kind, String title, String description, LocalDate startsOn,
        LocalDate endsOn, String globalId, String groupId, String groupName) { }
    private record Counts(long linked, long proofs, long participants, long participatingGroups) { }
    public record Summary(long totalCount, long activeCount, long upcomingCount, long endedCount) { }
    public record Item(String id, String kind, String title, String description, LocalDate startsOn, LocalDate endsOn,
        String globalWeeklyChallengeId, String groupId, String groupName, String phase, long linkedGroupCount,
        long proofCount, long participantCount, long participatingGroupCount) { }
    public record Page(List<Item> items, long totalCount, Summary summary, boolean hasMore, String nextCursor,
        LocalDate from, LocalDate to, String timezone) { }
}
