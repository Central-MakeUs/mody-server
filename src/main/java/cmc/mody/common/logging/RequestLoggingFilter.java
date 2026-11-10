package cmc.mody.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 requestId를 MDC에 넣고, 끝나면 한 줄짜리 접근 로그를 남긴다.
 * 같은 요청에서 나온 로그는 rid 로 묶어 Loki에서 한 번에 찾는다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private static final long SLOW_REQUEST_MILLIS = 1_000;
    private static final Pattern SECRET_QUERY_PARAM =
        Pattern.compile("(?i)(^|&)(code|state|token|[a-z]*Token|key)=[^&]*");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/health")
            || uri.startsWith("/actuator")
            || uri.startsWith("/docs")
            || uri.startsWith("/swagger")
            || uri.startsWith("/v3/api-docs")
            || uri.startsWith("/.well-known");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(LogKeys.REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            logAccess(request, response.getStatus(), durationMillis);
            MDC.remove(LogKeys.REQUEST_ID);
            MDC.remove(LogKeys.MEMBER_ID);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String supplied = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(supplied) && SAFE_REQUEST_ID.matcher(supplied).matches()) {
            return supplied;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void logAccess(HttpServletRequest request, int status, long durationMillis) {
        String query = request.getQueryString();
        String path = query == null
            ? request.getRequestURI()
            : request.getRequestURI() + "?" + SECRET_QUERY_PARAM.matcher(query).replaceAll("$1$2=***");
        String userAgent = request.getHeader("User-Agent");
        if (status >= 500 || durationMillis >= SLOW_REQUEST_MILLIS) {
            log.warn("access method={} path={} status={} durationMs={} ua=\"{}\"",
                request.getMethod(), path, status, durationMillis, userAgent);
            return;
        }
        log.info("access method={} path={} status={} durationMs={} ua=\"{}\"",
            request.getMethod(), path, status, durationMillis, userAgent);
    }
}
