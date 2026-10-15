package com.altneb.platform.web;

import java.util.Objects;

public record ApiResponse<T>(boolean success, T data, String traceId) {

    public ApiResponse {
        if (!success) {
            throw new IllegalArgumentException("A success response must have success=true");
        }
        data = Objects.requireNonNull(data, "data");
        traceId = Objects.requireNonNull(traceId, "traceId");
    }

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(true, data, traceId);
    }
}
