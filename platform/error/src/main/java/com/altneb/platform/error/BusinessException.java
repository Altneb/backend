package com.altneb.platform.error;

import java.util.List;
import java.util.Objects;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public BusinessException(ErrorCode errorCode, List<ErrorDetail> details) {
        super(Objects.requireNonNull(errorCode, "errorCode").message());
        this.errorCode = errorCode;
        this.details = List.copyOf(Objects.requireNonNull(details, "details"));
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}
