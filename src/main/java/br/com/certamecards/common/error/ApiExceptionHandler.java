package br.com.certamecards.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        ApiError body = ApiError.of(ex.getErrorCode(), ex.getDetail(), ex.getFields(), ex.getRetryAfterSeconds());
        ResponseEntity.BodyBuilder response =
                ResponseEntity.status(ex.getErrorCode().status());
        if (ex.getRetryAfterSeconds() != null) {
            response.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        }
        return response.body(body);
    }

    @ExceptionHandler(CardLimitException.class)
    public ResponseEntity<ApiError> handleCardLimit(CardLimitException ex) {
        return ResponseEntity.status(ex.getErrorCode().status()).body(ApiError.ofCardLimit(ex));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiFieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), errorCodeOf(error), error.getDefaultMessage()))
                .toList();
        ApiError body = ApiError.of(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), fields, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        ApiError body = ApiError.of(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), null, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequestParameters(Exception ex) {
        ApiError body = ApiError.of(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), null, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiError body = new ApiError(
                "https://certamecards.com.br/erros/internal_error",
                "Erro interno",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "internal_error",
                "Erro inesperado. Tente de novo.",
                null,
                null,
                null,
                null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String errorCodeOf(org.springframework.validation.FieldError error) {
        return error.getCode() == null ? "invalid" : error.getCode();
    }
}
