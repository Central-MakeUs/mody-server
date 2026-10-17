package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리 화면 지표. 활동은 식사·운동 기록, 챌린지 인증, 댓글 작성이다(자동 동기화되는 걸음 수는 제외).
 * 날짜 함수가 DB마다 달라 회원·활동 날짜만 조회하고 집계는 메모리에서 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {
    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DAILY_DAYS = 30;
    private static final int RETENTION_WEEKS = 8;
    private final NamedParameterJdbcTemplate jdbc;
    private Clock clock = Clock.system(KST);

    void useClock(Clock clock) {
        this.clock = clock;
    }

    public Stats stats(int weeks) {
        if (weeks < 1 || weeks > 26) {
            throw new GeneralException(ErrorStatus.BAD_REQUEST);
        }
        LocalDate today = LocalDate.now(clock.withZone(KST));
        LocalDate cohortStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks - 1);
        LocalDate activityFrom = cohortStart.isBefore(today.minusDays(DAILY_DAYS)) ? cohortStart
            : today.minusDays(DAILY_DAYS);
        Map<Long, Set<LocalDate>> activity = activity(activityFrom);
        Map<LocalDate, Set<Long>> byDay = new HashMap<>();
        activity.forEach((member, days) -> days.forEach(day ->
            byDay.computeIfAbsent(day, key -> new HashSet<>()).add(member)));

        return new Stats(today, active(byDay, today), daily(byDay, today), funnel(today, activity),
            cohorts(cohortStart, today, weeks, activity));
    }

    private Map<Long, Set<LocalDate>> activity(LocalDate from) {
        var params = new MapSqlParameterSource("from", from.atStartOfDay());
        Map<Long, Set<LocalDate>> activity = new HashMap<>();
        jdbc.query("""
            SELECT member_id, CAST(uploaded_at AS DATE) AS active_on FROM activity_record
                WHERE uploaded_at >= :from AND status = 'ACTIVE' AND deleted_at IS NULL
            UNION
            SELECT member_id, CAST(uploaded_at AS DATE) AS active_on FROM challenge_proof
                WHERE uploaded_at >= :from AND status = 'ACTIVE' AND deleted_at IS NULL
            UNION
            SELECT member_id, CAST(created_at AS DATE) AS active_on FROM record_comment
                WHERE created_at >= :from AND status = 'ACTIVE' AND deleted_at IS NULL
            """, params, rs -> {
            activity.computeIfAbsent(rs.getLong("member_id"), key -> new HashSet<>())
                .add(rs.getDate("active_on").toLocalDate());
        });
        return activity;
    }

    private Active active(Map<LocalDate, Set<Long>> byDay, LocalDate today) {
        long dau = distinct(byDay, today, today);
        long wau = distinct(byDay, today.minusDays(6), today);
        long mau = distinct(byDay, today.minusDays(29), today);
        return new Active(dau, wau, mau, mau == 0 ? 0 : Math.round(dau * 1000.0 / mau) / 10.0);
    }

    private long distinct(Map<LocalDate, Set<Long>> byDay, LocalDate from, LocalDate to) {
        Set<Long> members = new HashSet<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            members.addAll(byDay.getOrDefault(day, Set.of()));
        }
        return members.size();
    }

    private List<DailyActive> daily(Map<LocalDate, Set<Long>> byDay, LocalDate today) {
        List<DailyActive> series = new ArrayList<>();
        for (LocalDate day = today.minusDays(DAILY_DAYS - 1); !day.isAfter(today); day = day.plusDays(1)) {
            series.add(new DailyActive(day, byDay.getOrDefault(day, Set.of()).size()));
        }
        return series;
    }

    /** 최근 30일 가입자가 각 단계까지 도달했는지. 기록 단계는 기간과 무관하게 가입 이후 전체를 본다. */
    private Funnel funnel(LocalDate today, Map<Long, Set<LocalDate>> recentActivity) {
        LocalDate since = today.minusDays(29);
        var params = new MapSqlParameterSource("since", since.atStartOfDay());
        return jdbc.queryForObject("""
            SELECT COUNT(*) AS signed_up,
                SUM(CASE WHEN m.birth_date IS NOT NULL AND m.target_weight_kg IS NOT NULL THEN 1 ELSE 0 END)
                    AS profile_completed,
                SUM(CASE WHEN EXISTS (SELECT 1 FROM group_member gm WHERE gm.member_id = m.id) THEN 1 ELSE 0 END)
                    AS joined_group,
                SUM(CASE WHEN EXISTS (SELECT 1 FROM activity_record r WHERE r.member_id = m.id)
                    OR EXISTS (SELECT 1 FROM challenge_proof p WHERE p.member_id = m.id) THEN 1 ELSE 0 END)
                    AS first_record,
                SUM(CASE WHEN m.status = 'ACTIVE' AND m.deleted_at IS NULL THEN 1 ELSE 0 END) AS still_member
            FROM member m WHERE m.created_at >= :since
            """, params, (rs, n) -> {
            List<Long> ids = jdbc.queryForList("SELECT id FROM member WHERE created_at >= :since", params, Long.class);
            long activeLast7 = ids.stream().filter(id -> recentActivity.getOrDefault(id, Set.of()).stream()
                .anyMatch(day -> !day.isBefore(today.minusDays(6)))).count();
            return new Funnel(since, rs.getLong("signed_up"), rs.getLong("profile_completed"),
                rs.getLong("joined_group"), rs.getLong("first_record"), activeLast7, rs.getLong("still_member"));
        });
    }

    /**
     * 가입 주(월요일 시작) 코호트별로, 가입일 기준 k주차(가입일+7k ~ +7k+6)에 활동한 회원 비율.
     * 아직 끝나지 않은 주차는 null이다.
     */
    private List<Cohort> cohorts(LocalDate start, LocalDate today, int weeks, Map<Long, Set<LocalDate>> activity) {
        var params = new MapSqlParameterSource("from", start.atStartOfDay());
        Map<LocalDate, List<long[]>> members = new HashMap<>();
        jdbc.query("SELECT id, CAST(created_at AS DATE) AS joined FROM member WHERE created_at >= :from", params,
            rs -> {
                LocalDate joined = rs.getDate("joined").toLocalDate();
                LocalDate week = joined.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                members.computeIfAbsent(week, key -> new ArrayList<>())
                    .add(new long[] {rs.getLong("id"), joined.toEpochDay()});
            });
        List<Cohort> cohorts = new ArrayList<>();
        for (int index = 0; index < weeks; index++) {
            LocalDate week = start.plusWeeks(index);
            List<long[]> cohort = members.getOrDefault(week, List.of());
            List<Double> retention = new ArrayList<>();
            for (int k = 0; k < RETENTION_WEEKS; k++) {
                int offset = k;
                long eligible = cohort.stream()
                    .filter(m -> !LocalDate.ofEpochDay(m[1]).plusDays(7L * offset + 6).isAfter(today)).count();
                if (cohort.isEmpty() || eligible < cohort.size()) {
                    retention.add(null);
                    continue;
                }
                long returned = cohort.stream().filter(m -> {
                    LocalDate from = LocalDate.ofEpochDay(m[1]).plusDays(7L * offset);
                    LocalDate to = from.plusDays(6);
                    return activity.getOrDefault(m[0], Set.of()).stream()
                        .anyMatch(day -> !day.isBefore(from) && !day.isAfter(to));
                }).count();
                retention.add(Math.round(returned * 1000.0 / cohort.size()) / 10.0);
            }
            cohorts.add(new Cohort(week, cohort.size(), retention));
        }
        return cohorts;
    }

    public record Active(long dau, long wau, long mau, double stickinessPercent) { }

    public record DailyActive(LocalDate date, long count) { }

    public record Funnel(LocalDate since, long signedUp, long profileCompleted, long joinedGroup, long firstRecord,
        long activeLast7Days, long stillMember) { }

    public record Cohort(LocalDate weekStart, long size, List<Double> retentionPercent) { }

    public record Stats(LocalDate today, Active active, List<DailyActive> dailyActive, Funnel funnel,
        List<Cohort> cohorts) { }
}
