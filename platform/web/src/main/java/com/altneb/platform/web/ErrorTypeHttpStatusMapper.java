package com.altneb.platform.web;

import com.altneb.platform.error.ErrorType;
import org.springframework.http.HttpStatus;

final class ErrorTypeHttpStatusMapper {

    private ErrorTypeHttpStatusMapper() {
    }

    static HttpStatus toHttpStatus(ErrorType errorType) {
        return switch (errorType) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case AUTHENTICATION -> HttpStatus.UNAUTHORIZED;
            case AUTHORIZATION -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case OPERATION_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_CONTENT_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_CONTENT;
            case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
            case DEPENDENCY_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
