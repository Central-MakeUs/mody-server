package cmc.mody.common.api.status;

import cmc.mody.common.api.BaseCode;

public enum ErrorStatus implements BaseCode {
    INTERNAL_SERVER_ERROR(500, "COMMON500", "서버 내부 오류 발생"),
    BAD_REQUEST(400, "COMMON4000", "잘못된 요청입니다."),
    UNAUTHORIZED(401, "COMMON4001", "인증되지 않은 요청입니다."),
    FORBIDDEN(403, "COMMON4002", "접근이 거부되었습니다."),
    VALIDATION_FAILED(400, "COMMON4003", "입력값이 올바르지 않습니다."),
    NO_AUTHORIZED(401, "AUTH401", "권한이 없습니다."),
    EMPTY_JWT(401, "AUTH402", "JWT 토큰이 비어있습니다."),
    INVALID_JWT(401, "AUTH403", "유효하지 않은 JWT token입니다."),
    EXPIRED_JWT(401, "AUTH404", "만료된 JWT token입니다."),
    UNSUPPORTED_JWT(401, "AUTH405", "지원되지 않는 JWT token입니다."),
    INVALID_REFRESH_TOKEN(401, "AUTH406", "유효하지 않은 리프레시 토큰입니다."),
    UNSUPPORTED_LOGIN_TYPE(400, "AUTH407", "지원하지 않는 소셜 로그인 타입입니다."),
    INVALID_OAUTH_TOKEN(400, "AUTH408", "유효하지 않은 OAuth 토큰입니다."),
    OAUTH_PROFILE_REQUEST_FAILED(400, "AUTH409", "OAuth 프로필 조회에 실패했습니다."),
    INVALID_OAUTH_PROFILE(400, "AUTH410", "OAuth 프로필 정보가 올바르지 않습니다."),
    MEMBER_SIGNUP_VALIDATION_FAILED(400, "MEMBER301", "회원 가입 입력값이 올바르지 않습니다."),
    MEMBER_NOT_FOUND(404, "MEMBER302", "회원을 찾을 수 없습니다."),
    MEMBER_PROFILE_ALREADY_COMPLETED(409, "MEMBER303", "이미 개인 정보 입력이 완료된 회원입니다."),
    MEMBER_NICKNAME_INVALID(400, "MEMBER304", "닉네임은 14자 이하로 입력해야 합니다."),
    MEMBER_BIRTH_DATE_INVALID(400, "MEMBER305", "생년월일은 과거 날짜로 입력해야 합니다."),
    MEMBER_WEIGHT_INVALID(400, "MEMBER306", "체중은 20kg 이상 300kg 이하로 입력해야 합니다."),
    MEMBER_MEAL_SCHEDULE_INVALID(400, "MEMBER307", "식사 설정은 아침, 점심, 저녁을 각각 1개씩 입력해야 합니다."),
    MEMBER_MEAL_TIME_INVALID(400, "MEMBER308", "먹지 않음이면 시간을 비우고, 먹는 식사는 시간을 입력해야 합니다."),
    MEMBER_EXERCISE_SCHEDULE_INVALID(400, "MEMBER309", "운동 일정은 최소 3개 이상 입력해야 합니다."),
    GROUP_VALIDATION_FAILED(400, "GROUP301", "그룹 입력값이 올바르지 않습니다."),
    GROUP_NOT_FOUND(404, "GROUP302", "그룹을 찾을 수 없습니다."),
    GROUP_CODE_GENERATION_FAILED(409, "GROUP303", "그룹 코드를 생성할 수 없습니다."),
    GROUP_LIMIT_EXCEEDED(409, "GROUP304", "참여 가능한 그룹 수를 초과했습니다."),
    GROUP_ALREADY_JOINED(409, "GROUP305", "이미 참여 중인 그룹입니다."),
    GROUP_MEMBER_NOT_FOUND(404, "GROUP306", "그룹 참여 정보를 찾을 수 없습니다."),
    GROUP_CAPACITY_EXCEEDED(409, "GROUP307", "그룹 최대 인원을 초과했습니다."),
    MYPAGE_VALIDATION_FAILED(400, "MYPAGE301", "마이페이지 입력값이 올바르지 않습니다."),
    MYPAGE_SOCIAL_ACCOUNT_NOT_FOUND(404, "MYPAGE302", "소셜 계정 정보를 찾을 수 없습니다."),
    RECORD_VALIDATION_FAILED(400, "RECORD301", "기록 입력값이 올바르지 않습니다."),
    RECORD_NOT_FOUND(404, "RECORD302", "기록을 찾을 수 없습니다."),
    RECORD_GROUP_ID_INVALID(400, "RECORD303", "그룹 id는 양수로 입력해야 합니다."),
    RECORD_TYPE_INVALID(400, "RECORD304", "기록 타입은 필수입니다."),
    RECORD_IMAGE_INVALID(400, "RECORD305", "기록 이미지는 records 도메인으로 발급된 이미지 키여야 합니다."),
    RECORD_MEAL_PAYLOAD_INVALID(400, "RECORD306", "식사 기록은 식사 시간과 메뉴가 필요합니다."),
    RECORD_EXERCISE_PAYLOAD_INVALID(400, "RECORD307", "운동 기록은 운동 시간과 운동명이 필요합니다."),
    RECORD_EXERCISE_DURATION_INVALID(400, "RECORD308", "운동 시간은 1분 이상 24시간 이하로 입력해야 합니다."),
    RECORD_COMMENT_INVALID(400, "RECORD309", "댓글은 1자 이상 100자 이하로 입력해야 합니다."),
    CHALLENGE_VALIDATION_FAILED(400, "CHALLENGE301", "챌린지 입력값이 올바르지 않습니다."),
    CHALLENGE_NOT_FOUND(404, "CHALLENGE302", "챌린지를 찾을 수 없습니다."),
    CHALLENGE_IN_PROGRESS_NOT_FOUND(404, "CHALLENGE303", "진행 중인 걸음수 챌린지를 찾을 수 없습니다."),
    CHALLENGE_PROOF_ALREADY_EXISTS(409, "CHALLENGE304", "이미 챌린지 인증을 완료했습니다."),
    CHALLENGE_ALREADY_COMPLETED(409, "CHALLENGE305", "이미 완료된 챌린지입니다."),
    CHALLENGE_NOT_COMPLETED(409, "CHALLENGE306", "완료되지 않은 챌린지입니다."),
    CHALLENGE_PROOF_NOT_FOUND(404, "CHALLENGE307", "챌린지 인증 이미지를 찾을 수 없습니다."),
    NOTIFICATION_UNSUPPORTED_TYPE(400, "NOTIFICATION301", "지원하지 않는 알림 타입입니다."),
    NOTIFICATION_NOT_FOUND(404, "NOTIFICATION302", "알림을 찾을 수 없습니다."),
    NOTIFICATION_PAYLOAD_INVALID(400, "NOTIFICATION303", "알림 요청값이 올바르지 않습니다."),
    NOTIFICATION_VALIDATION_FAILED(400, "NOTIFICATION304", "알림 입력값이 올바르지 않습니다."),
    NOTIFICATION_PUSH_CONFIG_INVALID(500, "NOTIFICATION501", "알림 발송 설정이 올바르지 않습니다."),
    UPLOAD_UNSUPPORTED_DOMAIN(400, "UPLOAD301", "지원하지 않는 업로드 도메인입니다."),
    UPLOAD_UNSUPPORTED_EXTENSION(400, "UPLOAD302", "지원하지 않는 파일 확장자입니다."),
    UPLOAD_STORAGE_CONFIG_INVALID(500, "UPLOAD303", "업로드 스토리지 설정이 올바르지 않습니다."),
    UPLOAD_PRESIGNED_URL_ISSUE_FAILED(500, "UPLOAD304", "업로드 URL 발급에 실패했습니다."),
    UPLOAD_STORAGE_OPERATION_FAILED(500, "UPLOAD305", "스토리지 이미지 처리에 실패했습니다."),
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
