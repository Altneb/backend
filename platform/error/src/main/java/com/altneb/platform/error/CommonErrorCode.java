package com.altneb.platform.error;

public enum CommonErrorCode implements ErrorCode {
    UNAUTHENTICATED("COMMON_008", "인증이 필요합니다.", ErrorType.AUTHENTICATION),
    ACCESS_DENIED("COMMON_009", "접근 권한이 없습니다.", ErrorType.AUTHORIZATION),
    INVALID_INPUT("COMMON_001", "입력값이 올바르지 않습니다.", ErrorType.VALIDATION),
    MALFORMED_REQUEST("COMMON_002", "요청 본문을 읽을 수 없습니다.", ErrorType.VALIDATION),
    RESOURCE_NOT_FOUND("COMMON_003", "요청한 리소스를 찾을 수 없습니다.", ErrorType.NOT_FOUND),
    METHOD_NOT_ALLOWED(
            "COMMON_004",
            "지원하지 않는 HTTP 메서드입니다.",
            ErrorType.OPERATION_NOT_ALLOWED
    ),
    UNSUPPORTED_MEDIA_TYPE(
            "COMMON_005",
            "지원하지 않는 미디어 타입입니다.",
            ErrorType.UNSUPPORTED_CONTENT_TYPE
    ),
    CONFLICT("COMMON_006", "요청이 현재 리소스 상태와 충돌합니다.", ErrorType.CONFLICT),
    INTERNAL_SERVER_ERROR("COMMON_007", "요청 처리 중 오류가 발생했습니다.", ErrorType.INTERNAL);

    private final String code;
    private final String message;
    private final ErrorType type;

    CommonErrorCode(String code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public ErrorType type() {
        return type;
    }
}
