package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 관리자 공지 푸시. 대상 회원을 계산해 이력을 남기고, 실제 발송은 {@link AdminAnnouncementDispatcher}가 비동기로 한다.
 * 앱 알림 목록에는 저장하지 않는다(앱이 모르는 알림 유형으로 목록이 깨지지 않게).
 */
@Service
@RequiredArgsConstructor
public class AdminAnnouncementService {
    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final LocalTime QUIET_START = LocalTime.of(21, 0);
    static final LocalTime QUIET_END = LocalTime.of(8, 0);
    private static final int MAX_TITLE = 50;
    private static final int MAX_CONTENT = 200;
    private static final int MAX_MEMBERS = 100;
    private static final String ACTIVE_MEMBER = "m.status = 'ACTIVE' AND m.deleted_at IS NULL";
    private final NamedParameterJdbcTemplate jdbc;
    private final IdGenerator ids;
    private final AdminAnnouncementDispatcher dispatcher;
    private Clock clock = Clock.system(KST);

    /** 테스트에서 야간 시간대 판단을 고정한다. */
    void useClock(Clock clock) {
        this.clock = clock;
    }

    public Preview preview(Target target) {
        Target normalized = normalize(target);
        Recipients recipients = recipients(normalized);
        return new Preview(normalized.type(), recipients.memberCount(), recipients.tokenCount(),
            isQuietHours(), groupName(normalized));
    }

    /**
     * 같은 idempotencyKey로 다시 요청하면 새로 보내지 않고 처음 결과를 돌려준다.
     */
    public Item send(Target target, String title, String content, boolean allowQuietHours, String idempotencyKey,
        String actor) {
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 64) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        List<Item> existing = find("a.idempotency_key = :key", new MapSqlParameterSource("key", idempotencyKey));
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }
        String safeTitle = text(title, MAX_TITLE);
        String safeContent = text(content, MAX_CONTENT);
        if (isQuietHours() && !allowQuietHours) {
            throw new GeneralException(ErrorStatus.ADMIN_ANNOUNCEMENT_QUIET_HOURS);
        }
        Target normalized = normalize(target);
        Recipients recipients = recipients(normalized);
        if (recipients.memberCount() == 0) {
            throw new GeneralException(ErrorStatus.ADMIN_ANNOUNCEMENT_NO_RECIPIENT);
        }
        long id = ids.nextId();
        try {
            jdbc.update("""
                INSERT INTO admin_announcement (id, title, content, target_type, target_detail, recipient_count,
                    token_count, invalid_token_count, send_status, idempotency_key, created_by, created_at)
                VALUES (:id, :title, :content, :type, :detail, :recipients, :tokens, 0, 'QUEUED', :key, :actor,
                    :createdAt)
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("title", safeTitle).addValue("content", safeContent)
                .addValue("type", normalized.type()).addValue("detail", detail(normalized))
                .addValue("recipients", recipients.memberCount()).addValue("tokens", recipients.tokenCount())
                .addValue("key", idempotencyKey).addValue("actor", actor)
                .addValue("createdAt", LocalDateTime.now(clock)));
        } catch (DuplicateKeyException exception) {
            return find("a.idempotency_key = :key", new MapSqlParameterSource("key", idempotencyKey)).getFirst();
        }
        dispatcher.dispatch(id, safeTitle, safeContent, tokenQuery(normalized), targetParams(normalized));
        return find("a.id = :id", new MapSqlParameterSource("id", id)).getFirst();
    }

    public Page list(String cursor, int size) {
        if (size < 1 || size > 100) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        var params = new MapSqlParameterSource().addValue("limit", size + 1);
        String where = "1 = 1";
        if (StringUtils.hasText(cursor)) {
            String[] parts = decode(cursor);
            where = "(a.created_at < :cursorAt OR (a.created_at = :cursorAt AND a.id < :cursorId))";
            params.addValue("cursorAt", LocalDateTime.parse(parts[0])).addValue("cursorId", Long.parseLong(parts[1]));
        }
        List<Item> rows = find(where + " ORDER BY a.created_at DESC, a.id DESC LIMIT :limit", params);
        boolean hasMore = rows.size() > size;
        List<Item> items = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? encode(items.getLast()) : null;
        return new Page(items, next, hasMore);
    }

    boolean isQuietHours() {
        LocalTime now = LocalTime.now(clock.withZone(KST));
        return !now.isBefore(QUIET_START) || now.isBefore(QUIET_END);
    }

    private List<Item> find(String where, MapSqlParameterSource params) {
        return jdbc.query("SELECT a.* FROM admin_announcement a WHERE " + where, params, (rs, n) -> new Item(
            Long.toString(rs.getLong("id")), rs.getString("title"), rs.getString("content"),
            rs.getString("target_type"), rs.getString("target_detail"), rs.getInt("recipient_count"),
            rs.getInt("token_count"), rs.getInt("invalid_token_count"), rs.getString("send_status"),
            rs.getString("error_message"), rs.getString("created_by"), time(rs, "created_at"),
            time(rs, "completed_at")));
    }

    private Target normalize(Target target) {
        if (target == null || !StringUtils.hasText(target.type())) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        String type = target.type().trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "ALL" -> new Target(type, null, List.of());
            case "GROUP" -> {
                if (target.groupId() == null) {
                    throw new GeneralException(ErrorStatus.BAD_REQUEST);
                }
                Integer groups = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM mody_group WHERE id = :id AND status = 'ACTIVE' AND deleted_at IS NULL
                    """, new MapSqlParameterSource("id", target.groupId()), Integer.class);
                if (groups == null || groups == 0) {
                    throw new GeneralException(ErrorStatus.GROUP_NOT_FOUND);
                }
                yield new Target(type, target.groupId(), List.of());
            }
            case "MEMBERS" -> {
                List<Long> memberIds = target.memberIds() == null ? List.of()
                    : List.copyOf(new LinkedHashSet<>(target.memberIds()));
                if (memberIds.isEmpty() || memberIds.size() > MAX_MEMBERS) {
                    throw new GeneralException(ErrorStatus.BAD_REQUEST);
                }
                yield new Target(type, null, memberIds);
            }
            default -> throw new GeneralException(ErrorStatus.BAD_REQUEST);
        };
    }

    private Recipients recipients(Target target) {
        MapSqlParameterSource params = targetParams(target);
        Integer members = jdbc.queryForObject("SELECT COUNT(*) FROM member m WHERE " + memberFilter(target),
            params, Integer.class);
        Integer tokens = jdbc.queryForObject("SELECT COUNT(*) FROM (" + tokenQuery(target) + ") t", params,
            Integer.class);
        return new Recipients(members == null ? 0 : members, tokens == null ? 0 : tokens);
    }

    /** 대상 회원의 수신 가능한 FCM 토큰. 발송 시점에 다시 실행해 최신 토큰을 쓴다. */
    private String tokenQuery(Target target) {
        return """
            SELECT DISTINCT t.fcm_token FROM member_push_token t JOIN member m ON m.id = t.member_id
            WHERE t.enabled = TRUE AND t.status = 'ACTIVE' AND t.deleted_at IS NULL AND
            """ + memberFilter(target);
    }

    private String memberFilter(Target target) {
        return switch (target.type()) {
            case "GROUP" -> ACTIVE_MEMBER + " AND EXISTS (SELECT 1 FROM group_member gm WHERE gm.member_id = m.id"
                + " AND gm.group_id = :groupId AND gm.group_member_status = 'JOINED'"
                + " AND gm.status = 'ACTIVE' AND gm.deleted_at IS NULL)";
            case "MEMBERS" -> ACTIVE_MEMBER + " AND m.id IN (:memberIds)";
            default -> ACTIVE_MEMBER;
        };
    }

    private MapSqlParameterSource targetParams(Target target) {
        return new MapSqlParameterSource()
            .addValue("groupId", target.groupId())
            .addValue("memberIds", target.memberIds().isEmpty() ? List.of(-1L) : target.memberIds());
    }

    private String groupName(Target target) {
        if (!"GROUP".equals(target.type())) {
            return null;
        }
        return jdbc.queryForObject("SELECT name FROM mody_group WHERE id = :id",
            new MapSqlParameterSource("id", target.groupId()), String.class);
    }

    private String detail(Target target) {
        return switch (target.type()) {
            case "GROUP" -> target.groupId().toString();
            case "MEMBERS" -> target.memberIds().stream().map(String::valueOf).collect(Collectors.joining(","));
            default -> null;
        };
    }

    private static String text(String value, int max) {
        if (!StringUtils.hasText(value) || value.trim().length() > max) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String encode(Item item) {
        String raw = item.createdAt() + "|" + item.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String[] decode(String cursor) {
        try {
            String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|");
            LocalDateTime.parse(parts[0]);
            Long.parseLong(parts[1]);
            return parts;
        } catch (RuntimeException exception) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
    }

    public record Target(String type, Long groupId, List<Long> memberIds) {
        public Target {
            memberIds = memberIds == null ? List.of() : memberIds;
        }
    }

    private record Recipients(int memberCount, int tokenCount) { }

    public record Preview(String targetType, int recipientCount, int tokenCount, boolean quietHours,
        String groupName) { }

    public record Item(String id, String title, String content, String targetType, String targetDetail,
        int recipientCount, int tokenCount, int invalidTokenCount, String status, String errorMessage,
        String createdBy, LocalDateTime createdAt, LocalDateTime completedAt) { }

    public record Page(List<Item> items, String nextCursor, boolean hasMore) { }
}
