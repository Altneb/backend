package com.altneb.platform.error;

/**
 * Stable, transport-independent error contract exposed by the application.
 */
public interface ErrorCode {

    String code();

    String message();

    ErrorType type();
}
