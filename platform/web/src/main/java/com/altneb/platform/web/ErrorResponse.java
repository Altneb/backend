package com.altneb.platform.web;

import com.altneb.platform.error.ErrorCode;
import com.altneb.platform.error.ErrorDetail;
import java.util.List;
import java.util.Objects;

public record ErrorResponse(boolean success, ErrorBody error, String traceId) {

    public ErrorResponse {
        if (success) {
            throw new IllegalArgumentException("An error response must have success=false");
        }
        error = Objects.requireNonNull(error, "error");
        traceId = Objects.requireNonNull(traceId, "traceId");
    }

    public static ErrorResponse of(ErrorCode errorCode, List<ErrorDetail> details, String traceId) {
        Objects.requireNonNull(errorCode, "errorCode");
        return new ErrorResponse(
                false,
                new ErrorBody(errorCode.code(), errorCode.message(), details),
                traceId
        );
    }
}
