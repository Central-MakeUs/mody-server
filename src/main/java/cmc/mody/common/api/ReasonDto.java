package cmc.mody.common.api;

public record ReasonDto(
    boolean isSuccess,
    String code,
    String message,
    int httpStatus
) {
}
