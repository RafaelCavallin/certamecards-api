package br.com.certamecards.common.error;

import java.util.List;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailOverride;
    private final Integer retryAfterSeconds;
    private final List<ApiFieldError> fields;

    protected ApiException(
            ErrorCode errorCode, String detailOverride, Integer retryAfterSeconds, List<ApiFieldError> fields) {
        super(errorCode.code());
        this.errorCode = errorCode;
        this.detailOverride = detailOverride;
        this.retryAfterSeconds = retryAfterSeconds;
        this.fields = fields;
    }

    public static ApiException of(ErrorCode errorCode) {
        return new ApiException(errorCode, null, null, null);
    }

    public static ApiException withDetail(ErrorCode errorCode, String detail) {
        return new ApiException(errorCode, detail, null, null);
    }

    public static ApiException withRetryAfter(ErrorCode errorCode, int retryAfterSeconds) {
        return new ApiException(errorCode, null, retryAfterSeconds, null);
    }

    public static ApiException withFieldError(ErrorCode errorCode, String detail, ApiFieldError field) {
        return new ApiException(errorCode, detail, null, List.of(field));
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetail() {
        return detailOverride != null ? detailOverride : errorCode.detail();
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public List<ApiFieldError> getFields() {
        return fields;
    }
}
