package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminSessionService;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class AdminRequestInterceptor implements HandlerInterceptor {
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final AdminSessionService sessions;
    private final AdminAccessService access;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String key = request.getHeader("X-Admin-Api-Key");
        if (key != null) {
            // Machine clients retain their existing contract; a wrong key never falls back to a session.
            access.validate(key);
            return true;
        }
        AdminAccount account = sessions.requireAccount(request);
        if (!READ_METHODS.contains(request.getMethod())) {
            sessions.validateCsrf(request);
            if (!account.getRole().canWrite()) {
                throw new GeneralException(ErrorStatus.FORBIDDEN);
            }
        }
        request.setAttribute(AdminSessionService.PRINCIPAL_ATTRIBUTE, account);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
        Object handler, Exception exception) {
        if (!READ_METHODS.contains(request.getMethod())) {
            AdminAccount account = (AdminAccount) request.getAttribute(AdminSessionService.PRINCIPAL_ATTRIBUTE);
            log.info("admin operation actor={} method={} path={} status={}",
                account == null ? "machine" : account.getId(), request.getMethod(),
                request.getRequestURI(), response.getStatus());
        }
    }
}
