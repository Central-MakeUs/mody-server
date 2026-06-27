package cmc.mody.common.api.status;

import cmc.mody.common.api.BaseCode;

public enum ErrorStatus implements BaseCode {
    INTERNAL_SERVER_ERROR(500, "COMMON500", "서버 내부 오류 발생"),
    BAD_REQUEST(400, "COMMON4000", "잘못된 요청입니다."),
    UNAUTHORIZED(401, "COMMON4001", "인증되지 않은 요청입니다."),
    FORBIDDEN(403, "COMMON4002", "접근이 거부되었습니다."),
    VALIDATION_FAILED(400, "COMMON4003", "입력값이 올바르지 않습니다."),
    EMPTY_JWT(400, "COMMON4005", "JWT 토큰이 비어있습니다."),
    INVALID_JWT(400, "COMMON4006", "유효하지 않은 JWT token입니다."),
    EXPIRED_JWT(400, "COMMON4007", "만료된 JWT token입니다."),
    UNSUPPORTED_JWT(400, "COMMON4008", "지원되지 않는 JWT token입니다."),
    NO_AUTHORIZED(401, "COMMON4009", "권한이 없습니다."),
    INVALID_REFRESH_TOKEN(400, "COMMON4010", "유효하지 않은 리프레시 토큰입니다."),
    UNSUPPORTED_LOGIN_TYPE(400, "AUTH4001", "지원하지 않는 소셜 로그인 타입입니다."),
    INVALID_OAUTH_TOKEN(400, "AUTH4002", "유효하지 않은 OAuth 토큰입니다."),
    OAUTH_PROFILE_REQUEST_FAILED(400, "AUTH4003", "OAuth 프로필 조회에 실패했습니다."),
    INVALID_OAUTH_PROFILE(400, "AUTH4004", "OAuth 프로필 정보가 올바르지 않습니다."),
    NOT_FOUND(404, "COMMON404", "리소스를 찾을 수 없습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;

    ErrorStatus(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }
}
