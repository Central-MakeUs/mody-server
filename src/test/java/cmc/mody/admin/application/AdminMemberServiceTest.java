package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.auth.domain.RefreshToken;
import cmc.mody.challenge.domain.StepRecord;
import cmc.mody.challenge.domain.StepSource;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.member.domain.LoginType;
import cmc.mody.member.domain.Member;
import cmc.mody.member.domain.SocialAccount;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.PushPlatform;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.report.domain.RecordReport;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminMemberServiceTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 11, 12, 0);
    @Autowired private EntityManager em;
    @Autowired private AdminMemberService members;

    @BeforeEach
    void setup() {
        em.persist(new Member(10L, "민지", LocalDate.of(2000, 1, 1), null));
        em.persist(new Member(11L, "하늘_100%", null, null));
        Member withdrawn = new Member(12L, "떠난회원", null, null);
        em.persist(withdrawn);
        withdrawn.delete();
        em.persist(new SocialAccount(20L, 10L, LoginType.KAKAO, "kakao-secret-id"));
        em.persist(new SocialAccount(21L, 11L, LoginType.APPLE, "apple-secret-id"));
        em.persist(new SocialAccount(22L, 12L, LoginType.KAKAO, "kakao-old"));
        em.persist(new ModyGroup(30L, "AAAABBBB", "아침 산책"));
        em.persist(new GroupMember(31L, 10L, 30L, AT));
        em.persist(meal(40L, 10L));
        em.persist(meal(41L, 10L));
        em.persist(meal(42L, 11L));
        em.persist(new RecordComment(50L, 42L, 30L, 10L, "좋아요"));
        em.persist(new RecordReport(60L, 11L, 40L));
        em.persist(new StepRecord(70L, 99L, 10L, AT.toLocalDate(), 8000, StepSource.values()[0]));
        em.persist(new MemberPushToken(80L, 10L, "device-1", PushPlatform.IOS, "fcm-secret-token", AT));
        em.persist(new RefreshToken(90L, 10L, "refresh-secret"));
        em.flush();
    }

    @Test
    void listFiltersByStateLoginTypeAndKeywordWithLikeEscaping() {
        assertThat(members.list(null, "ALL", null, null, 30).items()).hasSize(3);
        assertThat(members.list(null, "WITHDRAWN", null, null, 30).items())
            .extracting(AdminMemberService.Item::id).containsExactly("12");
        assertThat(members.list(null, "ACTIVE", "KAKAO", null, 30).items())
            .extracting(AdminMemberService.Item::id).containsExactly("10");
        assertThat(members.list("_100%", null, null, null, 30).items())
            .extracting(AdminMemberService.Item::id).containsExactly("11");
        assertThat(members.list("%", null, null, null, 30).items())
            .extracting(AdminMemberService.Item::id).containsExactly("11");
        assertThat(members.list("10", null, null, null, 30).items())
            .extracting(AdminMemberService.Item::id).contains("10");

        var minji = members.list("민지", null, null, null, 30).items().getFirst();
        assertThat(minji.loginTypes()).containsExactly("KAKAO");
        assertThat(minji.groupCount()).isEqualTo(1);
        assertThat(minji.recordCount()).isEqualTo(2);
        assertThat(minji.active()).isTrue();
    }

    @Test
    void listPagesWithKeysetCursorWithoutDuplicates() {
        var first = members.list(null, null, null, null, 2);
        assertThat(first.items()).hasSize(2);
        assertThat(first.hasMore()).isTrue();
        var second = members.list(null, null, null, first.nextCursor(), 2);
        assertThat(second.items()).hasSize(1);
        assertThat(second.hasMore()).isFalse();
        assertThat(second.items().getFirst().id()).isNotIn(
            first.items().stream().map(AdminMemberService.Item::id).toList());
    }

    @Test
    void detailAggregatesActivityWithoutExposingSecrets() {
        var detail = members.detail(10L);
        assertThat(detail.logins()).extracting(AdminMemberService.Login::loginType).containsExactly("KAKAO");
        assertThat(detail.groups()).extracting(AdminMemberService.Group::name).containsExactly("아침 산책");
        assertThat(detail.counts().mealRecords()).isEqualTo(2);
        assertThat(detail.counts().comments()).isEqualTo(1);
        assertThat(detail.counts().totalSteps()).isEqualTo(8000);
        assertThat(detail.counts().reportsReceived()).isEqualTo(1);
        assertThat(detail.devices()).extracting(AdminMemberService.Device::platform).containsExactly("IOS");
        assertThat(detail.recentRecords()).hasSize(2);
        assertThat(detail.activeSessions()).isEqualTo(1);
        assertThat(detail.toString()).doesNotContain("secret", "fcm-secret-token");
        assertThat(members.detail(11L).counts().reportsMade()).isEqualTo(1);
    }

    @Test
    void summaryAndValidation() {
        var summary = members.summary();
        assertThat(summary.totalCount()).isEqualTo(3);
        assertThat(summary.activeCount()).isEqualTo(2);
        assertThat(summary.withdrawnCount()).isEqualTo(1);
        assertThatThrownBy(() -> members.detail(999L)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> members.list(null, "BANNED", null, null, 30)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> members.list(null, null, "NAVER", null, 30)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> members.list(null, null, null, "not-a-cursor", 30))
            .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> members.list(null, null, null, null, 101)).isInstanceOf(GeneralException.class);
    }

    private ActivityRecord meal(long id, long memberId) {
        return new ActivityRecord(id, memberId, 30L, RecordType.MEAL, LocalTime.NOON, "샐러드", null, null,
            "records/" + id + ".jpg", AT);
    }
}
