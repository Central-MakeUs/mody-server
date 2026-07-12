package cmc.mody.common.alert;

import static cmc.mody.auth.infrastructure.constants.JwtConstants.AUTHORIZATION_HEADER;
import static cmc.mody.auth.infrastructure.constants.JwtConstants.BEARER_PREFIX;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerErrorAlertService {
    private static final String UNKNOWN = "unknown";

    private final ServerErrorAlertProperties properties;
    private final ServerErrorAlertSender serverErrorAlertSender;
    private final TokenProvider tokenProvider;
    private final MemberRepository memberRepository;

    public void notify(Throwable exception, HttpServletRequest request, int statusCode, String errorCode) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getWebhookUrl())) {
            return;
        }

        try {
            ServerErrorAlert alert = buildAlert(exception, request, statusCode, errorCode);
            serverErrorAlertSender.send(properties.getWebhookUrl(), format(alert));
        } catch (Exception alertException) {
            log.warn("Failed to build server error alert: {}", alertException.getMessage());
        }
    }

    private ServerErrorAlert buildAlert(
        Throwable exception,
        HttpServletRequest request,
        int statusCode,
        String errorCode
    ) {
        MemberContext memberContext = resolveMember(request);
        return new ServerErrorAlert(
            statusCode,
            valueOrUnknown(errorCode),
            request.getMethod(),
            request.getRequestURI(),
            sanitizeQueryString(request.getQueryString()),
            clientIp(request),
            valueOrUnknown(request.getHeader("User-Agent")),
            memberContext.memberId(),
            memberContext.nickname(),
            exception.getClass().getName(),
            valueOrUnknown(exception.getMessage()),
            stackTrace(exception)
        );
    }

    private MemberContext resolveMember(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return MemberContext.unknown();
        }

        try {
            Long memberId = tokenProvider.getMemberIdByAccessToken(authorization.substring(BEARER_PREFIX.length()));
            String nickname = memberRepository.findById(memberId)
                .filter(Member::isActive)
                .map(Member::getNickname)
                .orElse(UNKNOWN);
            return new MemberContext(memberId, nickname);
        } catch (Exception e) {
            log.debug("Failed to resolve member for server error alert: {}", e.getMessage());
            return MemberContext.unknown();
        }
    }

    private String format(ServerErrorAlert alert) {
        String text = """
            :rotating_light: *500 Server Error*
            *Status*: `%d %s`
            *Member*: `%s / %s`
            *Request*: `%s %s%s`
            *Client IP*: `%s`
            *User-Agent*: `%s`
            *Exception*: `%s`
            *Message*: `%s`
            *Stack*
            ```%s```
            """.formatted(
            alert.statusCode(),
            alert.errorCode(),
            alert.memberId() == null ? UNKNOWN : alert.memberId(),
            alert.nickname(),
            alert.method(),
            alert.uri(),
            StringUtils.hasText(alert.queryString()) ? "?" + alert.queryString() : "",
            alert.clientIp(),
            alert.userAgent(),
            alert.exceptionClass(),
            alert.exceptionMessage(),
            alert.stackTrace()
        );
        return truncate(text);
    }

    private String sanitizeQueryString(String queryString) {
        if (!StringUtils.hasText(queryString)) {
            return "";
        }
        return Arrays.stream(queryString.split("&"))
            .map(this::sanitizeQueryParameter)
            .reduce((left, right) -> left + "&" + right)
            .orElse("");
    }

    private String sanitizeQueryParameter(String parameter) {
        int separator = parameter.indexOf('=');
        String key = separator < 0 ? parameter : parameter.substring(0, separator);
        String lowerKey = key.toLowerCase(Locale.ROOT);
        if (lowerKey.contains("token")
            || lowerKey.contains("code")
            || lowerKey.contains("secret")
            || lowerKey.contains("password")) {
            return key + "=REDACTED";
        }
        return parameter;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return valueOrUnknown(request.getRemoteAddr());
    }

    private String stackTrace(Throwable exception) {
        int depth = Math.max(1, properties.getStackTraceDepth());
        return Arrays.stream(exception.getStackTrace())
            .limit(depth)
            .map(StackTraceElement::toString)
            .reduce((left, right) -> left + "\n" + right)
            .orElse(UNKNOWN);
    }

    private String truncate(String text) {
        int maxLength = Math.max(500, properties.getMaxMessageLength());
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 15) + "\n...(truncated)";
    }

    private String valueOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN;
    }

    private record MemberContext(Long memberId, String nickname) {
        private static MemberContext unknown() {
            return new MemberContext(null, UNKNOWN);
        }
    }
}
