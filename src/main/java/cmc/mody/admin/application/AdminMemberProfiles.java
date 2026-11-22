package cmc.mody.admin.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 관리 화면 목록(회원, 인증 피드)에 붙이는 회원 맥락: 생년월일과 지금 참여 중인 그룹.
 * 목록 쿼리를 무겁게 만들지 않도록 화면에 나온 회원만 한 번에 따로 읽는다.
 */
@Component
@RequiredArgsConstructor
public class AdminMemberProfiles {
    private static final int MAX_MEMBERS = 500;
    private final NamedParameterJdbcTemplate jdbc;

    public Map<String, Profile> load(Collection<Long> memberIds) {
        List<Long> ids = memberIds.stream().distinct().limit(MAX_MEMBERS).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        var params = new MapSqlParameterSource("ids", ids);
        Map<Long, LocalDate> birthDates = new HashMap<>();
        jdbc.query("SELECT id, birth_date FROM member WHERE id IN (:ids)", params, rs -> {
            java.sql.Date date = rs.getDate("birth_date");
            birthDates.put(rs.getLong("id"), date == null ? null : date.toLocalDate());
        });
        Map<Long, List<GroupName>> groups = new HashMap<>();
        jdbc.query("""
            SELECT gm.member_id, g.id, g.name FROM group_member gm
            JOIN mody_group g ON g.id = gm.group_id AND g.status = 'ACTIVE' AND g.deleted_at IS NULL
            WHERE gm.member_id IN (:ids) AND gm.group_member_status = 'JOINED'
                AND gm.status = 'ACTIVE' AND gm.deleted_at IS NULL
            ORDER BY gm.joined_at
            """, params, rs -> {
            groups.computeIfAbsent(rs.getLong("member_id"), key -> new ArrayList<>())
                .add(new GroupName(Long.toString(rs.getLong("id")), rs.getString("name")));
        });
        Map<String, Profile> result = new HashMap<>();
        for (Long id : ids) {
            if (birthDates.containsKey(id)) {
                result.put(Long.toString(id), new Profile(birthDates.get(id), groups.getOrDefault(id, List.of())));
            }
        }
        return result;
    }

    public record GroupName(String id, String name) { }

    public record Profile(LocalDate birthDate, List<GroupName> groups) { }
}
