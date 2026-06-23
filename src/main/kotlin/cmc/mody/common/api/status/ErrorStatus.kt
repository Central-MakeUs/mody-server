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
    NOT_FOUND(404, "COMMON404", "리소스를 찾을 수 없습니다."),
    ;

    override val isSuccess: Boolean = false
}
