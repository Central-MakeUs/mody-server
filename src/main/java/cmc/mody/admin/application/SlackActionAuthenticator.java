package cmc.mody.admin.application;

import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.infrastructure.AdminAccountRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Slack 봇이 대신 보낸 신고 처리 요청을 확인하고, 버튼을 누른 Slack 사용자에 연결된 관리자 계정을 돌려준다(LLD-0040).
 * 봇은 Socket Mode로 받은 Slack 사용자 ID를 전용 비밀값으로 서명한다. 일반 관리자 API 키와는 다른 값이다.
 */
@Component
public class SlackActionAuthenticator {
    public static final String USER_HEADER = "X-Slack-User-Id";
    public static final String TIMESTAMP_HEADER = "X-Slack-Action-Timestamp";
    public static final String SIGNATURE_HEADER = "X-Slack-Action-Signature";
    private static final long TOLERANCE_SECONDS = 300;
    private static final Pattern SLACK_USER_ID = Pattern.compile("[A-Z0-9]{1,32}");
    private final byte[] secret;
    private final AdminAccountRepository accounts;
    private Clock clock = Clock.systemUTC();

    public SlackActionAuthenticator(@Value("${admin.slack-action.secret:}") String secret,
        AdminAccountRepository accounts) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accounts = accounts;
    }

    void useClock(Clock clock) {
        this.clock = clock;
    }

    public AdminAccount authenticate(HttpServletRequest request) {
        String user = request.getHeader(USER_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String signature = request.getHeader(SIGNATURE_HEADER);
        if (secret.length == 0 || user == null || timestamp == null || signature == null
            || !SLACK_USER_ID.matcher(user).matches() || !fresh(timestamp)) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        String expected = sign(secret, timestamp, request.getMethod(), request.getRequestURI(), user);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8))) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        AdminAccount account = accounts.findBySlackUserId(user)
            .filter(found -> found.isActive() && found.getDeletedAt() == null)
            .orElseThrow(() -> new GeneralException(ErrorStatus.FORBIDDEN));
        if (!account.getRole().canWrite()) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }
        return account;
    }

    private boolean fresh(String timestamp) {
        try {
            return Math.abs(clock.instant().getEpochSecond() - Long.parseLong(timestamp)) <= TOLERANCE_SECONDS;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    /** 봇(`src/report-actions.js`)과 같은 서명 문자열을 만든다. */
    public static String sign(byte[] secret, String timestamp, String method, String path, String slackUserId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String base = String.join(":", "v1", timestamp, method, path, slackUserId);
            return "v1=" + HexFormat.of().formatHex(mac.doFinal(base.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
