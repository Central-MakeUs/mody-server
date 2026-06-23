package cmc.mody.common.api.status

import cmc.mody.common.api.BaseCode

enum class ErrorStatus(
    override val httpStatus: Int,
    override val code: String,
    override val message: String
) : BaseCode {
    // 공통 에러 (도메인별 에러 코드는 기능 추가 시 함께 정의한다)
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
    NOT_FOUND(404, "COMMON404", "리소스를 찾을 수 없습니다."),
    ;

    override val isSuccess: Boolean = false
}
