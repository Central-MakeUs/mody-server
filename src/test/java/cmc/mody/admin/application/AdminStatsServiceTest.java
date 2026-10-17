package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.member.domain.Member;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminStatsServiceTest {
    // 2026-10-17(토) 12:00 KST
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
    private static final Clock CLOCK = Clock.fixed(TODAY.atTime(12, 0).atZone(AdminStatsService.KST).toInstant(),
        AdminStatsService.KST);
    @Autowired private EntityManager em;
    @Autowired private AdminStatsService stats;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        stats.useClock(CLOCK);
        // 9/28(월) 가입 2명: 10은 1주차와 2주차에 활동, 11은 가입 주에만 활동
        em.persist(new Member(10L, "민지", LocalDate.of(2000, 1, 1), new BigDecimal("55.0")));
        em.persist(new Member(11L, "하늘", null, null));
        // 오늘 가입 1명, 그룹 참여만 함
        em.persist(new Member(12L, "새싹", null, null));
        em.persist(new GroupMember(20L, 12L, 30L, TODAY.atTime(10, 0)));
        em.persist(record(100L, 10L, LocalDate.of(2026, 9, 28)));
        em.persist(record(101L, 10L, LocalDate.of(2026, 10, 6)));
        em.persist(record(102L, 10L, LocalDate.of(2026, 10, 17)));
        em.persist(record(103L, 11L, LocalDate.of(2026, 9, 29)));
        em.flush();
        joinedAt(10L, LocalDate.of(2026, 9, 28));
        joinedAt(11L, LocalDate.of(2026, 9, 28));
        joinedAt(12L, TODAY);
    }

    @Test
    void activeUsersAndDailySeries() {
        var result = stats.stats(8);
        assertThat(result.today()).isEqualTo(TODAY);
        assertThat(result.active().dau()).isEqualTo(1);
        assertThat(result.active().wau()).isEqualTo(1);
        assertThat(result.active().mau()).isEqualTo(2);
        assertThat(result.dailyActive()).hasSize(30);
        assertThat(result.dailyActive().getLast().count()).isEqualTo(1);
    }

    @Test
    void funnelCountsRecentSignupsByStep() {
        var funnel = stats.stats(8).funnel();
        assertThat(funnel.signedUp()).isEqualTo(3);
        assertThat(funnel.profileCompleted()).isEqualTo(1);
        assertThat(funnel.joinedGroup()).isEqualTo(1);
        assertThat(funnel.firstRecord()).isEqualTo(2);
        assertThat(funnel.activeLast7Days()).isEqualTo(1);
    }

    @Test
    void weeklyCohortRetentionLeavesUnfinishedWeeksEmpty() {
        var cohorts = stats.stats(8).cohorts();
        assertThat(cohorts).hasSize(8);
        var september = cohorts.stream().filter(c -> c.weekStart().equals(LocalDate.of(2026, 9, 28)))
            .findFirst().orElseThrow();
        assertThat(september.size()).isEqualTo(2);
        // 0주차: 둘 다 활동, 1주차(10/5~10/11): 10만, 2주차(10/12~18)는 아직 끝나지 않음
        assertThat(september.retentionPercent().subList(0, 3)).containsExactly(100.0, 50.0, null);
        var thisWeek = cohorts.getLast();
        assertThat(thisWeek.size()).isEqualTo(1);
        assertThat(thisWeek.retentionPercent()).containsOnlyNulls();
        assertThatThrownBy(() -> stats.stats(0)).isInstanceOf(GeneralException.class);
    }

    private void joinedAt(long memberId, LocalDate day) {
        jdbc.update("UPDATE member SET created_at = :at WHERE id = :id",
            new MapSqlParameterSource("at", day.atTime(9, 0)).addValue("id", memberId));
    }

    private ActivityRecord record(long id, long memberId, LocalDate day) {
        return new ActivityRecord(id, memberId, 30L, RecordType.MEAL, LocalTime.NOON, "샐러드", null, null,
            "records/" + id + ".jpg", LocalDateTime.of(day, LocalTime.of(12, 0)));
    }
}
