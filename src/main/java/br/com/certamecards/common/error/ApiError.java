package br.com.certamecards.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String type,
        String title,
        int status,
        String code,
        String detail,
        List<ApiFieldError> fields,
        Integer retryAfterSeconds,
        Integer requiredCards,
        Integer availableCards) {

    private static final String ERROR_BASE_URI = "https://certamecards.com.br/erros/";

    public static ApiError of(ErrorCode errorCode, String detail, List<ApiFieldError> fields, Integer retryAfter) {
        return new ApiError(
                ERROR_BASE_URI + errorCode.code(),
                errorCode.title(),
                errorCode.status().value(),
                errorCode.code(),
                detail,
                fields,
                retryAfter,
                null,
                null);
    }

    public static ApiError ofCardLimit(CardLimitException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return new ApiError(
                ERROR_BASE_URI + errorCode.code(),
                errorCode.title(),
                errorCode.status().value(),
                errorCode.code(),
                exception.getDetail(),
                null,
                null,
                exception.getRequiredCards(),
                exception.getAvailableCards());
    }
}
