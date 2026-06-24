package cmc.mody.common.api;

public interface BaseCode {
    int getHttpStatus();

    String getCode();

    String getMessage();

    boolean isSuccess();

    default ReasonDto toReasonDto() {
        return new ReasonDto(isSuccess(), getCode(), getMessage(), getHttpStatus());
    }
}
