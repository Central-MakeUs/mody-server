package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminAccountService;
import cmc.mody.admin.application.AdminGoogleIdentityVerifier;
import cmc.mody.admin.application.AdminSessionService;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.domain.AdminRole;
import cmc.mody.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AdminSessionService sessions;
    private final AdminAccountService accounts;
    private final AdminGoogleIdentityVerifier verifier;
    private final String clientId;
    private final Environment environment;

    public AdminAuthController(AdminSessionService sessions, AdminAccountService accounts,
        AdminGoogleIdentityVerifier verifier, @Value("${admin.google-client-id:}") String clientId,
        Environment environment) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.verifier = verifier;
        this.clientId = clientId;
        this.environment = environment;
    }

    @GetMapping("/config")
    public ApiResponse<Config> config(HttpServletRequest request) {
        var challenge = sessions.challenge(request);
        var profiles = java.util.Set.of(environment.getActiveProfiles());
        String profile = profiles.contains("prod") ? "prod" : profiles.contains("dev") ? "dev" : "local";
        return ApiResponse.ok(new Config(clientId, profile,
            challenge.csrfToken(), challenge.nonce()));
    }

    @PostMapping("/google")
    public ApiResponse<CurrentAdmin> login(@Valid @RequestBody GoogleLogin body, HttpServletRequest request) {
        sessions.validateCsrf(request);
        AdminAccount account = accounts.login(verifier.verify(body.credential(), sessions.consumeNonce(request)));
        sessions.login(request, account);
        return ApiResponse.ok(current(account, request));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentAdmin> me(HttpServletRequest request) {
        return ApiResponse.ok(current(sessions.requireAccount(request), request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        sessions.validateCsrf(request);
        request.getSession().invalidate();
        return ApiResponse.ok();
    }

    private CurrentAdmin current(AdminAccount account, HttpServletRequest request) {
        return new CurrentAdmin(account.getId().toString(), account.getEmail(), account.getRole(),
            sessions.csrf(request));
    }

    public record GoogleLogin(@NotBlank @Size(max = 8192) String credential) { }
    public record CurrentAdmin(String id, String email, AdminRole role, String csrfToken) { }
    public record Config(String googleClientId, String environment, String csrfToken, String nonce) { }
}
