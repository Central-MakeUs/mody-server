package cmc.mody.common.api;

import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.api.status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public record ApiResponse<T>(
    @JsonProperty("isSuccess")
    boolean isSuccess,
    String code,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    T result
) {
    public static <T> ApiResponse<T> success(SuccessStatus status, T result) {
        return new ApiResponse<>(true, status.getCode(), status.getMessage(), result);
    }

    public static ApiResponse<Void> success(SuccessStatus status) {
        return new ApiResponse<>(true, status.getCode(), status.getMessage(), null);
    }

    public static <T> ApiResponse<T> ok(T result) {
        return success(SuccessStatus.OK, result);
    }

    public static ApiResponse<Void> ok() {
        return success(SuccessStatus.OK);
    }

    public static <T> ApiResponse<T> created(T result) {
        return success(SuccessStatus.CREATED, result);
    }

    public static ApiResponse<Void> created() {
        return success(SuccessStatus.CREATED);
    }

    public static <T> ApiResponse<T> failure(ErrorStatus status, T result) {
        return new ApiResponse<>(false, status.getCode(), status.getMessage(), result);
    }

    public static ApiResponse<Void> failure(ErrorStatus status) {
        return new ApiResponse<>(false, status.getCode(), status.getMessage(), null);
    }

    public static <T> ApiResponse<T> failure(String code, String message, T result) {
        return new ApiResponse<>(false, code, message, result);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
