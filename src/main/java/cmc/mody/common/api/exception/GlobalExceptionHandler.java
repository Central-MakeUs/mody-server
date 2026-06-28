package cmc.mody.common.api.exception;

import cmc.mody.common.api.ApiResponse;
import cmc.mody.common.api.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.failure(resolveValidationStatus(request)));
    }

    private ErrorStatus resolveValidationStatus(HttpServletRequest request) {
        if ("/api/v1/onboarding/profile".equals(request.getRequestURI())) {
            return ErrorStatus.MEMBER_SIGNUP_VALIDATION_FAILED;
        }
        if (request.getRequestURI().startsWith("/api/v1/mypage")) {
            return ErrorStatus.MYPAGE_VALIDATION_FAILED;
        }
        return ErrorStatus.VALIDATION_FAILED;
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
