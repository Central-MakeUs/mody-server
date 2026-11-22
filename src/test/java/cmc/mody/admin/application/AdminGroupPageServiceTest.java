package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.member.domain.Member;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminGroupPageServiceTest {
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime AT = TODAY.atTime(9, 0);
    @Autowired private EntityManager em;
    @Autowired private AdminGroupPageService groups;

    @BeforeEach
    void setup() {
        em.persist(new Member(10L, "민지", LocalDate.of(2000, 1, 1), null));
        Member withdrawn = new Member(11L, "하늘", null, null);
        withdrawn.delete();
        em.persist(withdrawn);
        em.persist(new ModyGroup(30L, "AAAABBBB", "아침 산책"));
        em.persist(new ModyGroup(31L, "CCCCDDDD", "저녁 요가"));
        em.persist(new GroupMember(40L, 10L, 30L, AT.minusDays(3)));
        em.persist(new GroupMember(41L, 11L, 30L, AT.minusDays(2)));
        em.persist(new Challenge(50L, ChallengeType.PHOTO, "물 마시기", "물을 마셔요."));
        em.persist(new GroupChallenge(60L, 30L, 50L, TODAY.minusDays(2), TODAY.plusDays(4)));
        em.persist(new GroupChallenge(61L, 30L, 50L, TODAY.minusDays(10), TODAY.minusDays(4)));
        em.persist(new ActivityRecord(100L, 10L, 30L, RecordType.MEAL, LocalTime.NOON, "샐러드", null, null,
            "records/100.jpg", AT));
        em.persist(new ActivityRecordGroup(200L, 100L, 30L, 10L, AT));
        em.persist(new RecordComment(300L, 100L, 30L, 11L, "좋아요"));
        em.flush();
    }

    @Test
    void listSearchesByNameCodeOrId() {
        var all = groups.list(null, null, 30);
        assertThat(all.items()).extracting(AdminGroupPageService.Item::id).contains("30", "31");
        var morning = all.items().stream().filter(item -> item.id().equals("30")).findFirst().orElseThrow();
        assertThat(morning.memberCount()).isEqualTo(2);
        assertThat(morning.recordsLast7Days()).isEqualTo(1);

        assertThat(groups.list("아침", null, 30).items()).extracting(AdminGroupPageService.Item::id)
            .containsExactly("30");
        assertThat(groups.list("ccccdddd", null, 30).items()).extracting(AdminGroupPageService.Item::id)
            .containsExactly("31");
        assertThat(groups.list("31", null, 30).items()).extracting(AdminGroupPageService.Item::id)
            .containsExactly("31");

        var first = groups.list(null, null, 1);
        assertThat(first.hasMore()).isTrue();
        assertThat(groups.list(null, first.nextCursor(), 1).items()).hasSize(1);
        assertThatThrownBy(() -> groups.list(null, "bad", 1)).isInstanceOf(GeneralException.class);
    }

    @Test
    void detailGathersMembersChallengesAndComments() {
        var detail = groups.detail(30L);
        assertThat(detail.code()).isEqualTo("AAAABBBB");
        assertThat(detail.recordCount()).isEqualTo(1);
        assertThat(detail.commentCount()).isEqualTo(1);
        assertThat(detail.members()).extracting(AdminGroupPageService.Member::memberId).containsExactly("10", "11");
        assertThat(detail.members().getFirst().birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(detail.members().get(1).nickname()).isEqualTo("탈퇴한 회원");
        assertThat(detail.challenges()).extracting(AdminGroupPageService.Challenge::period)
            .containsExactly("CURRENT", "PAST");
        assertThat(detail.recentComments()).singleElement().satisfies(comment -> {
            assertThat(comment.recordTitle()).isEqualTo("샐러드");
            assertThat(comment.nickname()).isEqualTo("탈퇴한 회원");
        });
        assertThatThrownBy(() -> groups.detail(999L)).isInstanceOf(GeneralException.class);
    }
}
