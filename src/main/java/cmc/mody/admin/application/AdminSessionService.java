package cmc.mody.admin.application;

import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminSessionService {
    public static final String PRINCIPAL_ATTRIBUTE = AdminSessionService.class.getName() + ".principal";
    private static final String ACCOUNT = "admin.account", EXPIRES = "admin.expires";
    private static final String CSRF = "admin.csrf", NONCE = "admin.nonce";
    private final AdminAccountService accounts;

    public LoginChallenge challenge(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        if (session.getAttribute(CSRF) == null) {
            session.setAttribute(CSRF, UUID.randomUUID().toString());
        }
        // A new nonce makes previous login attempts unusable, but does not rotate the session CSRF token.
        session.setAttribute(NONCE, UUID.randomUUID().toString());
        return new LoginChallenge((String) session.getAttribute(CSRF), (String) session.getAttribute(NONCE));
    }

    public void validateCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String supplied = request.getHeader("X-CSRF-Token");
        String expected = session == null ? null : (String) session.getAttribute(CSRF);
        if (expected == null || supplied == null || !MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }
    }

    public String consumeNonce(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String nonce = null;
        if (session != null) {
            synchronized (session) {
                nonce = (String) session.getAttribute(NONCE);
                session.removeAttribute(NONCE);
            }
        }
        if (nonce == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        return nonce;
    }

    public void login(HttpServletRequest request, AdminAccount account) {
        request.changeSessionId();
        HttpSession session = request.getSession();
        session.setAttribute(ACCOUNT, account.getId());
        session.setAttribute(EXPIRES, Instant.now().plusSeconds(4 * 3600));
        session.setAttribute(CSRF, UUID.randomUUID().toString());
    }

    public AdminAccount requireAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long id = session == null ? null : (Long) session.getAttribute(ACCOUNT);
        Instant expires = session == null ? null : (Instant) session.getAttribute(EXPIRES);
        if (id == null || expires == null || !expires.isAfter(Instant.now())) {
            if (session != null) {
                session.invalidate();
            }
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        return accounts.getActive(id);
    }

    public String csrf(HttpServletRequest request) {
        return (String) request.getSession().getAttribute(CSRF);
    }

    public record LoginChallenge(String csrfToken, String nonce) { }
}
