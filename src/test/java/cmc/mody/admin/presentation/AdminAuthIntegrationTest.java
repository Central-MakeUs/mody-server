package cmc.mody.admin.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.admin.application.AdminGoogleIdentityVerifier;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.domain.AdminRole;
import cmc.mody.admin.infrastructure.AdminAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = { "admin.google-client-id=admin-web-test", "admin.api-key=machine-test-key",
    "admin.bootstrap-emails=allowed@gmail.com" })
@AutoConfigureMockMvc
@Transactional
class AdminAuthIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private AdminAccountRepository accounts;
    @MockitoBean private AdminGoogleIdentityVerifier verifier;
    private MockHttpSession session;
    private String csrf;

    @BeforeEach
    void setup() throws Exception {
        var response = mvc.perform(get("/api/v1/admin/auth/config"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        session = (MockHttpSession) response.getRequest().getSession(false);
        csrf = mapper.readTree(response.getResponse().getContentAsString()).at("/result/csrfToken").asText();
        given(verifier.verify(anyString(), anyString())).willReturn(
            new AdminGoogleIdentityVerifier.Identity("google-sub-1", "allowed@gmail.com", true));
    }

    @Test
    void anonymousUsersCannotReadOldOrNewAdminApisButMachineKeyStillWorks() throws Exception {
        mvc.perform(get("/api/v1/admin/feed")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/feed/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/challenge-overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/groups")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/groups").header("X-Admin-Api-Key", "machine-test-key"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/groups").header("X-Admin-Api-Key", "wrong"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginRotatesSessionAndCsrfAndAllowsExistingApisWithoutSharedKey() throws Exception {
        String previousId = session.getId();
        login();
        assertThat(session.getId()).isNotEqualTo(previousId);
        assertThat(csrf).isNotEqualTo(session.getAttribute("admin.csrf"));
        mvc.perform(get("/api/v1/admin/auth/me").session(session))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.email").value("allowed@gmail.com"))
            .andExpect(jsonPath("$.result.role").value("ADMIN"));
        mvc.perform(get("/api/v1/admin/groups").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/feed").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/feed/summary").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/challenge-overview").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/groups").session(session).header("X-Admin-Api-Key", "wrong"))
            .andExpect(status().isForbidden());
    }

    @Test
    void loginRequiresCsrfAndAuthoritativeAllowlistedGoogleAccount() throws Exception {
        mvc.perform(post("/api/v1/admin/auth/google").session(session)
            .contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"sample\"}"))
            .andExpect(status().isForbidden());
        given(verifier.verify(anyString(), anyString())).willReturn(
            new AdminGoogleIdentityVerifier.Identity("other-sub", "other@gmail.com", true));
        performLogin().andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/auth/config").session(session)).andExpect(status().isOk());
        given(verifier.verify(anyString(), anyString())).willReturn(
            new AdminGoogleIdentityVerifier.Identity("other-sub", "allowed@gmail.com", false));
        performLogin().andExpect(status().isForbidden());
        assertThat(accounts.findByGoogleSubject("other-sub")).isEmpty();
    }

    @Test
    void viewerCannotWriteAndSessionRevocationIsImmediate() throws Exception {
        accounts.saveAndFlush(new AdminAccount(987L, "google-sub-1", "allowed@gmail.com", AdminRole.VIEWER));
        login();
        mvc.perform(get("/api/v1/admin/groups").session(session)).andExpect(status().isOk());
        String token = (String) session.getAttribute("admin.csrf");
        mvc.perform(put("/api/v1/admin/groups/123").session(session).header("X-CSRF-Token", token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"changed\"}"))
            .andExpect(status().isForbidden());
        var account = accounts.findByGoogleSubject("google-sub-1").orElseThrow();
        account.delete();
        accounts.flush();
        mvc.perform(get("/api/v1/admin/groups").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void writesAndLogoutNeedCurrentCsrfAndSessionsHaveAbsoluteExpiry() throws Exception {
        login();
        mvc.perform(put("/api/v1/admin/groups/123").session(session)
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"changed\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/auth/logout").session(session).header("X-CSRF-Token", csrf))
            .andExpect(status().isForbidden());
        session.setAttribute("admin.expires", Instant.now().minusSeconds(1));
        mvc.perform(get("/api/v1/admin/feed").session(session)).andExpect(status().isUnauthorized());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void logoutInvalidatesSessionAndLoginNonceIsSingleUse() throws Exception {
        login();
        performLogin().andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/auth/logout").session(session)
            .header("X-CSRF-Token", session.getAttribute("admin.csrf")))
            .andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
    }

    private void login() throws Exception {
        performLogin().andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions performLogin() throws Exception {
        return mvc.perform(post("/api/v1/admin/auth/google").session(session).header("X-CSRF-Token", csrf)
            .contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"sample\"}"));
    }
}
