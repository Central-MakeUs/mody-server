package cmc.mody.common.api.status;

import cmc.mody.common.api.BaseCode;

public enum SuccessStatus implements BaseCode {
    OK(200, "COMMON200", "OK"),
    CREATED(201, "COMMON201", "생성 완료"),
    ACCEPTED(202, "COMMON202", "요청 수락됨"),
    NO_CONTENT(204, "COMMON204", "콘텐츠 없음");

    private final int httpStatus;
    private final String code;
    private final String message;

    SuccessStatus(int httpStatus, String code, String message) {
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
        return true;
    }
}
