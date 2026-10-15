package com.altneb.platform.observability;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.slf4j.MDC;

public final class TraceContext {

    public static final String HEADER_NAME = "X-Trace-Id";
    public static final String REQUEST_ATTRIBUTE = "altneb.traceId";
    public static final String MDC_KEY = "traceId";

    private TraceContext() {
    }

    public static String traceId(HttpServletRequest request) {
        return Optional.ofNullable(request.getAttribute(REQUEST_ATTRIBUTE))
                .map(Object::toString)
                .orElseGet(() -> Optional.ofNullable(MDC.get(MDC_KEY)).orElse("unknown"));
    }
}
