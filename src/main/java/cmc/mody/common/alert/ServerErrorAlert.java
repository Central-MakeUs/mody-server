package cmc.mody.common.alert;

public record ServerErrorAlert(
    int statusCode,
    String errorCode,
    String method,
    String uri,
    String queryString,
    String clientIp,
    String userAgent,
    Long memberId,
    String nickname,
    String exceptionClass,
    String exceptionMessage,
    String stackTrace
) {
}
