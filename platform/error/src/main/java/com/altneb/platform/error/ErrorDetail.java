package com.altneb.platform.error;

import java.util.Objects;

/**
 * Safe field-level context that can be returned to an API client.
 */
public record ErrorDetail(String field, String reason) {

    public ErrorDetail {
        field = Objects.requireNonNull(field, "field");
        reason = Objects.requireNonNull(reason, "reason");
    }
}
