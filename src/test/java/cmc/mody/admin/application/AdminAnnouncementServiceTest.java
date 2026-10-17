package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cmc.mody.admin.application.AdminAnnouncementService.Target;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.member.domain.Member;
import cmc.mody.notification.application.PushNotificationClient;
import cmc.mody.notification.application.PushNotificationResult;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.domain.PushPlatform;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminAnnouncementServiceTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 11, 12, 0);
    private static final Clock NOON = Clock.fixed(Instant.parse("2026-10-17T03:00:00Z"), AdminAnnouncementService.KST);
    private static final Clock NIGHT = Clock.fixed(Instant.parse("2026-10-17T14:00:00Z"), AdminAnnouncementService.KST);
    @Autowired private EntityManager em;
    @Autowired private AdminAnnouncementService announcements;
    @Autowired private NamedParameterJdbcTemplate jdbc;
    @MockBean private AdminAnnouncementDispatcher dispatcher;

    @BeforeEach
    void setup() {
        announcements.useClock(NOON);
        em.persist(new Member(10L, "민지", null, null));
        em.persist(new Member(11L, "하늘", null, null));
        Member withdrawn = new Member(12L, "탈퇴", null, null);
        em.persist(withdrawn);
        withdrawn.delete();
        em.persist(new ModyGroup(20L, "AAAABBBB", "아침 산책"));
        em.persist(new GroupMember(30L, 10L, 20L, AT));
        em.persist(new MemberPushToken(40L, 10L, "d1", PushPlatform.IOS, "token-a", AT));
        em.persist(new MemberPushToken(41L, 11L, "d2", PushPlatform.ANDROID, "token-b", AT));
        em.persist(new MemberPushToken(42L, 12L, "d3", PushPlatform.ANDROID, "token-c", AT));
        em.flush();
    }

    @Test
    void previewCountsOnlyActiveRecipientsAndTheirTokens() {
        var all = announcements.preview(new Target("ALL", null, null));
        assertThat(all.recipientCount()).isEqualTo(2);
        assertThat(all.tokenCount()).isEqualTo(2);
        var group = announcements.preview(new Target("group", 20L, null));
        assertThat(group.recipientCount()).isEqualTo(1);
        assertThat(group.groupName()).isEqualTo("아침 산책");
        assertThat(announcements.preview(new Target("MEMBERS", null, List.of(11L, 12L, 11L))).recipientCount())
            .isEqualTo(1);
        assertThatThrownBy(() -> announcements.preview(new Target("GROUP", 999L, null)))
            .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> announcements.preview(new Target("MEMBERS", null, List.of())))
            .isInstanceOf(GeneralException.class);
    }

    @Test
    void sendRecordsHistoryOncePerIdempotencyKeyAndDispatches() {
        var first = announcements.send(new Target("ALL", null, null), " 점검 안내 ", "오늘 밤 점검이 있어요.", false,
            "key-1", "ops@example.com");
        var again = announcements.send(new Target("ALL", null, null), "다른 제목", "다른 내용", false, "key-1",
            "ops@example.com");
        assertThat(again.id()).isEqualTo(first.id());
        assertThat(first.title()).isEqualTo("점검 안내");
        assertThat(first.recipientCount()).isEqualTo(2);
        assertThat(first.status()).isEqualTo("QUEUED");
        verify(dispatcher, times(1)).dispatch(anyLong(), anyString(), anyString(), anyString(),
            any(MapSqlParameterSource.class));
        assertThat(announcements.list(null, 20).items()).hasSize(1);
        assertThat(em.createQuery("select count(n) from Notification n", Long.class).getSingleResult()).isZero();
    }

    @Test
    void quietHoursAndValidationBlockSending() {
        announcements.useClock(NIGHT);
        assertThatThrownBy(() -> announcements.send(new Target("ALL", null, null), "제목", "내용", false, "k2",
            "ops")).isInstanceOf(GeneralException.class);
        assertThat(announcements.send(new Target("ALL", null, null), "제목", "내용", true, "k3", "ops").id())
            .isNotNull();
        announcements.useClock(NOON);
        assertThatThrownBy(() -> announcements.send(new Target("ALL", null, null), "x".repeat(51), "내용", false,
            "k4", "ops")).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> announcements.send(new Target("MEMBERS", null, List.of(12L)), "제목", "내용", false,
            "k5", "ops")).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> announcements.send(new Target("ALL", null, null), "제목", "내용", false, null,
            "ops")).isInstanceOf(GeneralException.class);
        verify(dispatcher, times(1)).dispatch(anyLong(), anyString(), anyString(), anyString(),
            any(MapSqlParameterSource.class));
    }

    @Test
    void dispatcherSendsWithoutPersistingNotificationAndDisablesInvalidTokens() {
        jdbc.update("""
            INSERT INTO admin_announcement (id, title, content, target_type, recipient_count, token_count,
                invalid_token_count, send_status, idempotency_key, created_by, created_at)
            VALUES (500, '제목', '내용', 'ALL', 2, 2, 0, 'QUEUED', 'k', 'ops', CURRENT_TIMESTAMP)
            """, new MapSqlParameterSource());
        PushNotificationClient push = mock(PushNotificationClient.class);
        when(push.send(any(Notification.class), anyList())).thenReturn(new PushNotificationResult(List.of("token-b")));
        var direct = new AdminAnnouncementDispatcher(jdbc, push);
        var params = new MapSqlParameterSource().addValue("groupId", null).addValue("memberIds", List.of(-1L));
        direct.dispatch(500L, "제목", "내용", """
            SELECT DISTINCT t.fcm_token FROM member_push_token t JOIN member m ON m.id = t.member_id
            WHERE t.enabled = TRUE AND t.status = 'ACTIVE' AND t.deleted_at IS NULL
                AND m.status = 'ACTIVE' AND m.deleted_at IS NULL
            """, params);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Notification> message = ArgumentCaptor.forClass(Notification.class);
        verify(push).send(message.capture(), tokens.capture());
        assertThat(tokens.getValue()).containsExactlyInAnyOrder("token-a", "token-b");
        assertThat(message.getValue().getNotificationType()).isEqualTo(NotificationType.ADMIN_ANNOUNCEMENT);
        em.clear();
        assertThat(em.find(MemberPushToken.class, 41L).isEnabled()).isFalse();
        var item = announcements.list(null, 20).items().getFirst();
        assertThat(item.status()).isEqualTo("SENT");
        assertThat(item.invalidTokenCount()).isEqualTo(1);
        verify(dispatcher, never()).dispatch(anyLong(), anyString(), anyString(), anyString(), any());
    }
}
