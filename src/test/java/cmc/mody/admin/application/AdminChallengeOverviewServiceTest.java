package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeProof;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GlobalWeeklyChallenge;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.member.domain.Member;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminChallengeOverviewServiceTest {
    private static final LocalDate DAY = LocalDate.now(ZoneId.of("Asia/Seoul"));
    @Autowired private EntityManager em;
    @Autowired private AdminChallengeOverviewService overview;

    @BeforeEach
    void setup() {
        em.persist(new Member(10L, "회원", null, null));
        em.persist(new Member(11L, "다른 회원", null, null));
        em.persist(new ModyGroup(20L, "AAAABBBB", "첫 그룹"));
        em.persist(new ModyGroup(21L, "CCCCDDDD", "둘째 그룹"));
        em.persist(new Challenge(30L, ChallengeType.PHOTO, "물 한 잔", "사진 인증"));
        em.persist(new Challenge(31L, ChallengeType.PHOTO, "물 한 잔", "사진 인증"));
        em.persist(new Challenge(32L, ChallengeType.PHOTO, "그룹 스트레칭", "운동 인증"));
        em.persist(new GlobalWeeklyChallenge(50L, 30L, DAY, DAY.plusDays(6)));
        em.persist(new GroupChallenge(40L, 20L, 30L, 50L, DAY, DAY.plusDays(6)));
        em.persist(new GroupChallenge(41L, 21L, 31L, 50L, DAY, DAY.plusDays(6)));
        em.persist(new GroupChallenge(42L, 20L, 32L, DAY, DAY.plusDays(6)));
        em.persist(new ChallengeProof(100L, 40L, 10L, "one.jpg", DAY.atTime(12, 0)));
        em.persist(new ChallengeProof(101L, 41L, 10L, "two.jpg", DAY.atTime(12, 0)));
        em.persist(new ChallengeProof(102L, 40L, 11L, "outside.jpg", DAY.minusDays(1).atTime(12, 0)));
        em.flush();
    }

    @Test
    void globalRowsAggregateGroupsWithoutDuplicatingParticipantsAndExcludeOutsideDates() {
        var page = list(null, "ALL", null, null, 24);
        assertThat(page.totalCount()).isEqualTo(2);
        var global = page.items().stream().filter(item -> item.kind().equals("GLOBAL")).findFirst().orElseThrow();
        assertThat(global.linkedGroupCount()).isEqualTo(2);
        assertThat(global.proofCount()).isEqualTo(2);
        assertThat(global.participantCount()).isEqualTo(1);
        assertThat(global.participatingGroupCount()).isEqualTo(2);
        assertThat(global.phase()).isEqualTo("ACTIVE");
    }

    @Test
    void groupScopeIncludesBothGlobalAndLegacyChallenges() {
        var page = list(20L, "ALL", null, null, 24);
        assertThat(page.items()).hasSize(2).extracting(AdminChallengeOverviewService.Item::id)
            .containsExactly("42", "40");
        var shared = page.items().get(1);
        assertThat(shared.globalWeeklyChallengeId()).isEqualTo("50");
        assertThat(shared.proofCount()).isEqualTo(1);
        assertThat(shared.groupId()).isEqualTo("20");
        assertThat(list(21L, "ALL", null, null, 24).items()).hasSize(1);
    }

    @Test
    void paginationAndSearchHaveIndependentTotalCounts() {
        List<String> seen = new ArrayList<>();
        String cursor = null;
        do {
            var page = list(null, "ALL", null, cursor, 1);
            assertThat(page.totalCount()).isEqualTo(2);
            page.items().forEach(item -> seen.add(item.kind() + ":" + item.id()));
            cursor = page.nextCursor();
        } while (cursor != null);
        assertThat(seen).containsExactly("GROUP:42", "GLOBAL:50");
        assertThat(list(null, "ALL", "물", null, 24).items()).hasSize(1);
        assertThat(list(null, "ALL", "%", null, 24).items()).isEmpty();
    }

    @Test
    void phaseAndDeletionRespectCalendarDatesAndActiveEntities() {
        em.persist(new Challenge(33L, ChallengeType.PHOTO, "다음 주", "예약"));
        em.persist(new GlobalWeeklyChallenge(51L, 33L, DAY.plusDays(7), DAY.plusDays(13)));
        em.find(GroupChallenge.class, 41L).delete();
        em.flush();
        var upcoming = list(null, "UPCOMING", null, null, 24);
        assertThat(upcoming.items()).hasSize(1);
        assertThat(upcoming.totalCount()).isEqualTo(1);
        assertThat(upcoming.summary().activeCount()).isEqualTo(2);
        var global = list(null, "ALL", "물", null, 24).items().getFirst();
        assertThat(global.linkedGroupCount()).isEqualTo(1);
        assertThat(global.proofCount()).isEqualTo(1);
        em.find(Challenge.class, 30L).delete();
        em.flush();
        assertThat(list(null, "ALL", "물", null, 24).items()).isEmpty();
    }

    @Test
    void rejectsInvalidFiltersCursorsAndMissingGroups() {
        assertThatThrownBy(() -> list(null, "INVALID", null, null, 24)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> list(null, "ALL", null, "broken", 24)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> list(999L, "ALL", null, null, 24)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> list(null, "ALL", null, null, 101)).isInstanceOf(GeneralException.class);
    }

    private AdminChallengeOverviewService.Page list(Long group, String phase, String keyword, String cursor, int size) {
        return overview.list(DAY.minusDays(7), DAY.plusDays(21), group, phase, keyword, cursor, size);
    }
}
