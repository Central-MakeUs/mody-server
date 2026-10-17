package cmc.mody.admin.application;

import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 관리 화면과 관리자 API 키로 들어온 변경 요청(GET/HEAD/OPTIONS 제외)을 남기고 조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditService {
    static final String MACHINE = "machine";
    private static final int MAX_PATH = 500;
    private final NamedParameterJdbcTemplate jdbc;
    private final IdGenerator ids;

    /** 이력 저장 실패가 원래 요청 결과를 바꾸지 않도록 예외를 삼킨다. */
    public void record(AdminAccount actor, String method, String path, int status) {
        try {
            String safePath = path == null ? "" : path.substring(0, Math.min(path.length(), MAX_PATH));
            jdbc.update("""
                INSERT INTO admin_audit_log
                    (id, actor_admin_id, actor_label, http_method, request_path, status_code, created_at)
                VALUES (:id, :actorId, :actor, :method, :path, :status, :createdAt)
                """, new MapSqlParameterSource()
                .addValue("id", ids.nextId())
                .addValue("actorId", actor == null ? null : actor.getId())
                .addValue("actor", actor == null ? MACHINE : actor.getEmail())
                .addValue("method", method)
                .addValue("path", safePath)
                .addValue("status", status)
                .addValue("createdAt", LocalDateTime.now()));
        } catch (RuntimeException exception) {
            log.warn("admin audit log write failed method={} path={}", method, path, exception);
        }
    }

    public Page list(String actor, String cursor, int size) {
        if (size < 1 || size > 100) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        var params = new MapSqlParameterSource().addValue("limit", size + 1);
        StringBuilder sql = new StringBuilder("""
            SELECT id, actor_label, http_method, request_path, status_code, created_at
            FROM admin_audit_log WHERE 1 = 1
            """);
        if (StringUtils.hasText(actor)) {
            sql.append(" AND actor_label = :actor");
            params.addValue("actor", actor.trim());
        }
        if (StringUtils.hasText(cursor)) {
            Cursor decoded = decode(cursor);
            sql.append(" AND (created_at < :cursorAt OR (created_at = :cursorAt AND id < :cursorId))");
            params.addValue("cursorAt", decoded.createdAt()).addValue("cursorId", decoded.id());
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT :limit");
        List<Item> rows = jdbc.query(sql.toString(), params, (rs, n) -> new Item(
            Long.toString(rs.getLong("id")), rs.getString("actor_label"), rs.getString("http_method"),
            rs.getString("request_path"), rs.getInt("status_code"),
            rs.getTimestamp("created_at").toLocalDateTime()));
        boolean hasMore = rows.size() > size;
        List<Item> items = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? encode(items.getLast()) : null;
        return new Page(items, next, hasMore);
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

    public record Item(String id, String actor, String method, String path, int status, LocalDateTime createdAt) { }

    public record Page(List<Item> items, String nextCursor, boolean hasMore) { }
}
