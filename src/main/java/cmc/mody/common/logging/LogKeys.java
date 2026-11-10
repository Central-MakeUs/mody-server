package cmc.mody.common.logging;

/** 로그 패턴(application.yaml logging.pattern.level)에서 쓰는 MDC 키. */
public final class LogKeys {
    public static final String REQUEST_ID = "requestId";
    public static final String MEMBER_ID = "memberId";

    private LogKeys() {
    }
}
