package cmc.mody.admin.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.admin.application.SlackActionAuthenticator;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.domain.AdminRole;
import cmc.mody.member.domain.Member;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.report.domain.CommentReport;
import cmc.mody.report.domain.RecordReport;
import cmc.mody.report.domain.RecordReportResolution;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = { "admin.api-key=machine-test-key", "admin.slack-action.secret=test-secret" })
@AutoConfigureMockMvc
@Transactional
class AdminSlackReportIntegrationTest {
    private static final byte[] SECRET = "test-secret".getBytes(StandardCharsets.UTF_8);
    private static final String RECORD_HIDE = "/api/v1/admin/slack/reports/records/100/hide";
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setup() {
        em.persist(account(1L, "operator@example.com", AdminRole.OPERATOR, "UOPERATOR"));
        em.persist(account(2L, "viewer@example.com", AdminRole.VIEWER, "UVIEWER"));
        em.persist(account(3L, "admin@example.com", AdminRole.ADMIN, "UADMIN"));
        em.persist(account(4L, "console@example.com", AdminRole.ADMIN, null));
        em.persist(new Member(10L, "작성자", null, null));
        em.persist(new ActivityRecord(100L, 10L, 30L, RecordType.MEAL, LocalTime.NOON, "샐러드", null, null,
            "records/100.jpg", LocalDateTime.of(2026, 11, 25, 12, 0)));
        em.persist(new RecordComment(300L, 100L, 30L, 10L, "댓글"));
        em.persist(new RecordReport(400L, 11L, 100L));
        em.persist(new RecordReport(401L, 12L, 100L));
        em.persist(new CommentReport(500L, 11L, 300L, 100L, 30L));
        em.flush();
    }

    @Test
    void operatorHidesRecordAndSecondClickSeesAlreadyResolved() throws Exception {
        mvc.perform(signed(RECORD_HIDE, "UOPERATOR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.outcome").value("RESOLVED"))
            .andExpect(jsonPath("$.result.kind").value("RECORD"))
            .andExpect(jsonPath("$.result.resolution").value("HIDDEN"))
            .andExpect(jsonPath("$.result.resolvedBy").value("operator@example.com"))
            .andExpect(jsonPath("$.result.resolverSlackUserId").value("UOPERATOR"))
            .andExpect(jsonPath("$.result.resolvedReports").value(2));
        em.flush();
        em.clear();
        assertThat(em.find(ActivityRecord.class, 100L).isActive()).isFalse();
        RecordReport report = em.find(RecordReport.class, 400L);
        assertThat(report.getResolution()).isEqualTo(RecordReportResolution.HIDDEN);
        assertThat(report.getResolvedBy()).isEqualTo("operator@example.com");
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM admin_audit_log
            WHERE actor_admin_id = 1 AND actor_label = 'operator@example.com' AND request_path = ?
              AND status_code = 200
            """, Long.class, RECORD_HIDE)).isEqualTo(1L);

        mvc.perform(signed("/api/v1/admin/slack/reports/records/100/dismiss", "UADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.outcome").value("ALREADY_RESOLVED"))
            .andExpect(jsonPath("$.result.resolution").value("HIDDEN"))
            .andExpect(jsonPath("$.result.resolvedBy").value("operator@example.com"))
            .andExpect(jsonPath("$.result.resolvedReports").value(0));
        em.flush();
        em.clear();
        assertThat(em.find(RecordReport.class, 401L).getResolution()).isEqualTo(RecordReportResolution.HIDDEN);
    }

    @Test
    void adminDismissesCommentAndConsoleThenSeesNothingPending() throws Exception {
        mvc.perform(signed("/api/v1/admin/slack/reports/comments/300/dismiss", "UADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.outcome").value("RESOLVED"))
            .andExpect(jsonPath("$.result.kind").value("COMMENT"))
            .andExpect(jsonPath("$.result.resolution").value("DISMISSED"));
        em.flush();
        em.clear();
        assertThat(em.find(RecordComment.class, 300L).isActive()).isTrue();
        mvc.perform(post("/api/v1/admin/reports/comments/300/hide").header("X-Admin-Api-Key", "machine-test-key"))
            .andExpect(status().isNotFound());
    }

    @Test
    void alreadyResolvedFromConsoleShowsConsoleResolver() throws Exception {
        em.find(RecordReport.class, 400L).resolve(RecordReportResolution.DISMISSED, "console@example.com",
            LocalDateTime.of(2026, 11, 25, 9, 0));
        em.find(RecordReport.class, 401L).resolve(RecordReportResolution.DISMISSED, "console@example.com",
            LocalDateTime.of(2026, 11, 25, 9, 0));
        em.flush();

        mvc.perform(signed(RECORD_HIDE, "UOPERATOR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.outcome").value("ALREADY_RESOLVED"))
            .andExpect(jsonPath("$.result.resolution").value("DISMISSED"))
            .andExpect(jsonPath("$.result.resolvedBy").value("console@example.com"))
            .andExpect(jsonPath("$.result.resolverSlackUserId").doesNotExist());
        em.flush();
        em.clear();
        assertThat(em.find(ActivityRecord.class, 100L).isActive()).isTrue();
    }

    @Test
    void viewerAndUnlinkedSlackUsersAreForbidden() throws Exception {
        mvc.perform(signed(RECORD_HIDE, "UVIEWER")).andExpect(status().isForbidden());
        mvc.perform(signed(RECORD_HIDE, "UNOBODY")).andExpect(status().isForbidden());
        em.flush();
        em.clear();
        assertThat(em.find(RecordReport.class, 400L).getResolution()).isNull();
    }

    @Test
    void badOrExpiredSignatureAndApiKeyAreRejectedOnSlackPath() throws Exception {
        String now = Long.toString(Instant.now().getEpochSecond());
        mvc.perform(post(RECORD_HIDE).header(SlackActionAuthenticator.USER_HEADER, "UOPERATOR")
                .header(SlackActionAuthenticator.TIMESTAMP_HEADER, now)
                .header(SlackActionAuthenticator.SIGNATURE_HEADER, "v1=deadbeef"))
            .andExpect(status().isUnauthorized());
        String old = Long.toString(Instant.now().getEpochSecond() - 600);
        mvc.perform(post(RECORD_HIDE).header(SlackActionAuthenticator.USER_HEADER, "UOPERATOR")
                .header(SlackActionAuthenticator.TIMESTAMP_HEADER, old)
                .header(SlackActionAuthenticator.SIGNATURE_HEADER,
                    SlackActionAuthenticator.sign(SECRET, old, "POST", RECORD_HIDE, "UOPERATOR")))
            .andExpect(status().isUnauthorized());
        // 다른 사용자로 만든 서명을 사용자만 바꿔 쓰면 통과하지 못한다.
        mvc.perform(post(RECORD_HIDE).header(SlackActionAuthenticator.USER_HEADER, "UADMIN")
                .header(SlackActionAuthenticator.TIMESTAMP_HEADER, now)
                .header(SlackActionAuthenticator.SIGNATURE_HEADER,
                    SlackActionAuthenticator.sign(SECRET, now, "POST", RECORD_HIDE, "UOPERATOR")))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(RECORD_HIDE).header("X-Admin-Api-Key", "machine-test-key"))
            .andExpect(status().isUnauthorized());
        em.flush();
        em.clear();
        assertThat(em.find(RecordReport.class, 400L).getResolution()).isNull();
    }

    @Test
    void slackSignatureIsNotAcceptedOnOtherAdminApis() throws Exception {
        String path = "/api/v1/admin/reports/records/100/hide";
        String now = Long.toString(Instant.now().getEpochSecond());
        mvc.perform(post(path).header(SlackActionAuthenticator.USER_HEADER, "UOPERATOR")
                .header(SlackActionAuthenticator.TIMESTAMP_HEADER, now)
                .header(SlackActionAuthenticator.SIGNATURE_HEADER,
                    SlackActionAuthenticator.sign(SECRET, now, "POST", path, "UOPERATOR")))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/reports").header(SlackActionAuthenticator.USER_HEADER, "UOPERATOR"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownTargetAndActionAreRejected() throws Exception {
        mvc.perform(signed("/api/v1/admin/slack/reports/records/999/hide", "UOPERATOR"))
            .andExpect(status().isNotFound());
        mvc.perform(signed("/api/v1/admin/slack/reports/records/100/delete", "UOPERATOR"))
            .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder signed(String path, String slackUserId) {
        String now = Long.toString(Instant.now().getEpochSecond());
        return post(path).header(SlackActionAuthenticator.USER_HEADER, slackUserId)
            .header(SlackActionAuthenticator.TIMESTAMP_HEADER, now)
            .header(SlackActionAuthenticator.SIGNATURE_HEADER,
                SlackActionAuthenticator.sign(SECRET, now, "POST", path, slackUserId));
    }

    private static AdminAccount account(long id, String email, AdminRole role, String slackUserId) {
        AdminAccount account = new AdminAccount(id, "google-" + id, email, role);
        if (slackUserId != null) {
            account.linkSlackUser(slackUserId);
        }
        return account;
    }
}
