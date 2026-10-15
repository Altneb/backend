package com.altneb.identity.error;

import com.altneb.platform.error.ErrorCode;
import com.altneb.platform.error.ErrorType;

public enum IdentityErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS("AUTH_001", "이미 사용 중인 이메일입니다.", ErrorType.CONFLICT),
    INVALID_CREDENTIALS("AUTH_002", "이메일 또는 비밀번호가 올바르지 않습니다.", ErrorType.AUTHENTICATION),
    INVALID_REFRESH_TOKEN("AUTH_003", "유효하지 않은 Refresh Token입니다.", ErrorType.AUTHENTICATION),
    EXPIRED_REFRESH_TOKEN("AUTH_004", "만료된 Refresh Token입니다.", ErrorType.AUTHENTICATION),
    REFRESH_TOKEN_REUSED("AUTH_005", "Refresh Token 재사용이 감지되었습니다.", ErrorType.AUTHENTICATION);

    private final String code;
    private final String message;
    private final ErrorType type;

    IdentityErrorCode(String code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    public String code() { return code; }
    public String message() { return message; }
    public ErrorType type() { return type; }
}
