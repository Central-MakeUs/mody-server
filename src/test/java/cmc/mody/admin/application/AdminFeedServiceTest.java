package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeProof;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.member.domain.Member;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordType;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminFeedServiceTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 11);
    private static final LocalDateTime AT = DAY.atTime(12, 0);
    @Autowired private EntityManager em;
    @Autowired private AdminFeedService feed;

    @BeforeEach
    void setup() {
        em.persist(new Member(10L, "민지", null, null));
        em.persist(new ModyGroup(20L, "AAAABBBB", "우리 그룹"));
        em.persist(new ModyGroup(21L, "CCCCDDDD", "친구 그룹"));
        em.persist(new Challenge(30L, ChallengeType.PHOTO, "건강한 한 끼", "사진을 남겨주세요."));
        em.persist(new GroupChallenge(40L, 20L, 30L, 50L, DAY, DAY.plusDays(6)));
        em.persist(activity(100L, RecordType.MEAL, AT));
        em.persist(activity(101L, RecordType.EXERCISE, AT));
        // IDs can overlap between independent source tables.
        em.persist(new ChallengeProof(100L, 40L, 10L, "challenges/100.jpg", AT));
        em.persist(new ActivityRecordGroup(200L, 100L, 20L, 10L, AT));
        em.persist(new ActivityRecordGroup(201L, 100L, 21L, 10L, AT));
        em.flush();
    }

    @Test
    void summaryCountsAllMatchingRecordsRatherThanTheLoadedPage() {
        var first = list(null, null, null, null, null, null, 1);
        assertThat(first.items()).hasSize(1);
        var total = feed.summary(DAY, DAY, null, null, null, null, null, null);
        assertThat(total.totalCount()).isEqualTo(3);
        assertThat(total.mealCount()).isEqualTo(1);
        assertThat(total.exerciseCount()).isEqualTo(1);
        assertThat(total.challengeCount()).isEqualTo(1);
        assertThat(feed.summary(DAY, DAY, null, 10L, 21L, null, null, "샐러드").totalCount()).isEqualTo(1);
        assertThat(feed.summary(DAY.plusDays(1), DAY.plusDays(1), null, null, null, null, null, null)
            .totalCount()).isZero();
    }

    @Test
    void globalChallengeAndActivityFiltersUseTheSameCriteriaForCountsAndPages() {
        assertThat(feed.list(DAY, DAY, null, null, null, null, 50L, null, null, 24).items())
            .extracting(AdminFeedService.Item::type).containsExactly("CHALLENGE");
        assertThat(feed.summary(DAY, DAY, null, null, null, null, 50L, null).totalCount()).isEqualTo(1);
        assertThat(feed.summary(DAY, DAY, "ACTIVITY", null, null, null, null, null).totalCount()).isEqualTo(2);
        assertThat(list("ACTIVITY", null, null, null, null, null, 24).items()).hasSize(2);
        em.find(GroupChallenge.class, 40L).delete();
        em.flush();
        assertThat(feed.summary(DAY, DAY, null, null, null, null, 50L, null).totalCount()).isZero();
        assertThatThrownBy(() -> feed.summary(DAY, DAY, null, null, null, null, -1L, null))
            .isInstanceOf(GeneralException.class);
    }

    @Test
    void summaryIsNotLimitedToTwentyFourAndLegacyRecordsAreReadOnly() {
        for (long id = 300; id < 330; id++) {
            em.persist(activity(id, RecordType.MEAL, AT.minusDays(1)));
        }
        em.flush();
        assertThat(feed.list(DAY.minusDays(1), DAY, null, null, null, null, null, null, 24).items())
            .hasSize(24);
        assertThat(feed.summary(DAY.minusDays(1), DAY, null, null, null, null, null, null).totalCount())
            .isEqualTo(33);
        assertThat(list("MEAL", null, 21L, null, null, null, 24).items().getFirst().groupRecordEditable()).isTrue();
        assertThat(list("EXERCISE", null, 99L, null, null, null, 24).items().getFirst().groupRecordEditable())
            .isFalse();
    }

    @Test
    void mixedSourcesAndSharedGroupsDoNotDuplicateOrLoseTiedTimestamps() {
        List<String> identities = new ArrayList<>();
        String cursor = null;
        do {
            var page = list(null, null, null, null, null, cursor, 1);
            page.items().forEach(item -> identities.add(item.type() + ":" + item.id()));
            cursor = page.nextCursor();
        } while (cursor != null);
        assertThat(identities).containsExactly("MEAL:100", "EXERCISE:101", "CHALLENGE:100");
    }

    @Test
    void filtersAndDetailsPreserveActualGroupMappings() {
        assertThat(list("MEAL", 10L, 21L, null, "샐러드", null, 24).items()).hasSize(1);
        assertThat(list("CHALLENGE", null, 20L, 40L, "건강", null, 24).items()).hasSize(1);
        assertThat(list("EXERCISE", null, null, 40L, null, null, 24).items()).isEmpty();
        assertThat(list(null, 999L, null, null, null, null, 24).items()).isEmpty();
        assertThat(list(null, null, null, null, "%", null, 24).items()).isEmpty();
        var detail = feed.detail("MEAL", 100L);
        assertThat(detail.groups()).extracting(AdminFeedService.Group::id).containsExactly("20", "21");
        assertThat(detail.item().imageUrl()).isEqualTo("https://storage.example.com/records/100.jpg");
        assertThat(feed.detail("CHALLENGE", 100L).item().globalWeeklyChallengeId()).isEqualTo("50");
        assertThat(feed.detail("CHALLENGE", 100L).groups())
            .extracting(AdminFeedService.Group::id).containsExactly("20");
    }

    @Test
    void dateBoundariesUseInclusiveCalendarDatesAndDeletedRecordsAreExcluded() {
        em.persist(activity(102L, RecordType.MEAL, DAY.atStartOfDay()));
        em.persist(activity(103L, RecordType.MEAL, DAY.plusDays(1).atStartOfDay()));
        ActivityRecord removed = activity(104L, RecordType.MEAL, AT.plusMinutes(1));
        removed.delete();
        em.persist(removed);
        em.flush();
        assertThat(list(null, null, null, null, null, null, 24).items())
            .extracting(AdminFeedService.Item::id).contains("102").doesNotContain("103", "104");
    }

    @Test
    void deletedMembersAreAnonymizedAndMissingOrMismatchedTypesReturnNotFound() {
        em.find(Member.class, 10L).delete();
        em.flush();
        assertThat(list(null, null, null, null, null, null, 24).items())
            .extracting(AdminFeedService.Item::nickname).containsOnly("탈퇴한 회원");
        assertThat(list(null, null, null, null, "민지", null, 24).items()).isEmpty();
        assertThatThrownBy(() -> feed.detail("EXERCISE", 100L))
            .isInstanceOfSatisfying(GeneralException.class,
                ex -> assertThat(ex.getStatus()).isEqualTo(ErrorStatus.NOT_FOUND));
    }

    @Test
    void mappingTakesPrecedenceOverLegacyGroupAndDeletedChallengesAreExcluded() {
        assertThat(list(null, null, 99L, null, null, null, 24).items()).hasSize(1);
        em.find(GroupChallenge.class, 40L).delete();
        em.flush();
        assertThat(list("CHALLENGE", null, null, null, null, null, 24).items()).isEmpty();
    }

    @Test
    void rejectsInvalidCursorAndUnboundedQueries() {
        assertThatThrownBy(() -> list(null, null, null, null, null, "broken", 24))
            .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> list("UNKNOWN", null, null, null, null, null, 24))
            .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> list(null, null, null, null, null, null, 101))
            .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> feed.list(DAY, DAY.minusDays(1), null, null, null, null, null, null, 24))
            .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> feed.list(DAY, DAY.plusDays(366), null, null, null, null, null, null, 24))
            .isInstanceOf(GeneralException.class);
    }

    private AdminFeedService.Page list(String type, Long member, Long group, Long challenge,
        String keyword, String cursor, int size) {
        return feed.list(DAY, DAY, type, member, group, challenge, keyword, cursor, size);
    }

    private ActivityRecord activity(long id, RecordType type, LocalDateTime at) {
        return new ActivityRecord(id, 10L, 99L, type, type == RecordType.MEAL ? LocalTime.NOON : null,
            type == RecordType.MEAL ? "샐러드" : null, type == RecordType.EXERCISE ? 30 : null,
            type == RecordType.EXERCISE ? "걷기" : null, "records/" + id + ".jpg", at);
    }
}
