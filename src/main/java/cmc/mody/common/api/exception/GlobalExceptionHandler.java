package cmc.mody.common.api.exception;

import cmc.mody.common.api.ApiResponse;
import cmc.mody.common.api.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(GeneralException e) {
        log.warn("GeneralException: {}", e.getMessage());
        return ResponseEntity
            .status(e.getStatus().getHttpStatus())
            .body(ApiResponse.failure(e.getStatus()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
        MethodArgumentNotValidException e,
        HttpServletRequest request
    ) {
        log.warn("Validation failed: {}", e.getBindingResult().getFieldErrors());
        ErrorStatus status = ValidationErrorStatusResolver.resolve(e, resolveValidationStatus(request));
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.failure(status));
    }

    private ErrorStatus resolveValidationStatus(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/v1/onboarding")) {
            return ErrorStatus.MEMBER_SIGNUP_VALIDATION_FAILED;
        }
        if (request.getRequestURI().startsWith("/api/v1/groups")) {
            return ErrorStatus.GROUP_VALIDATION_FAILED;
        }
        if (request.getRequestURI().startsWith("/api/v1/mypage")) {
            return ErrorStatus.MYPAGE_VALIDATION_FAILED;
        }
        if (request.getRequestURI().startsWith("/api/v1/records")) {
            return ErrorStatus.RECORD_VALIDATION_FAILED;
        }
        if (request.getRequestURI().contains("/challenges")
            || request.getRequestURI().contains("/weekly-challenges")) {
            return ErrorStatus.CHALLENGE_VALIDATION_FAILED;
        }
        if (request.getRequestURI().startsWith("/api/v1/notifications")) {
            return ErrorStatus.NOTIFICATION_VALIDATION_FAILED;
        }
        return ErrorStatus.VALIDATION_FAILED;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e,
        HttpServletRequest request
    ) {
        log.warn("Request body is not readable: {}", e.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.failure(resolveValidationStatus(request)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
        MissingServletRequestParameterException e
    ) {
        log.warn("Missing request parameter: {}", e.getParameterName());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.failure(ErrorStatus.BAD_REQUEST));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException e,
        HttpServletRequest request
    ) {
        log.warn("Request parameter type mismatch: name={}, value={}", e.getName(), e.getValue());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.failure(resolveValidationStatus(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        String message = e.getMessage() == null ? "잘못된 요청입니다." : e.getMessage();
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.failure(ErrorStatus.BAD_REQUEST.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity
            .internalServerError()
            .body(ApiResponse.failure(ErrorStatus.INTERNAL_SERVER_ERROR));
    }
}
