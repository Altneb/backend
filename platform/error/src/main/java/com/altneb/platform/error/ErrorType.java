package com.altneb.platform.error;

/**
 * Transport-independent classification of an application error.
 */
public enum ErrorType {
    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    NOT_FOUND,
    OPERATION_NOT_ALLOWED,
    UNSUPPORTED_CONTENT_TYPE,
    CONFLICT,
    UNPROCESSABLE,
    RATE_LIMIT,
    DEPENDENCY_UNAVAILABLE,
    INTERNAL
}
