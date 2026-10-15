package com.altneb.platform.web;

import com.altneb.platform.error.ErrorDetail;
import java.util.List;
import java.util.Objects;

public record ErrorBody(String code, String message, List<ErrorDetail> details) {

    public ErrorBody {
        code = Objects.requireNonNull(code, "code");
        message = Objects.requireNonNull(message, "message");
        details = List.copyOf(Objects.requireNonNull(details, "details"));
    }
}
