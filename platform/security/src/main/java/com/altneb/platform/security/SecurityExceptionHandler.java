package com.altneb.platform.security;

import com.altneb.platform.error.CommonErrorCode;
import com.altneb.platform.observability.TraceContext;
import com.altneb.platform.web.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            AuthorizationDeniedException exception,
            HttpServletRequest request
    ) {
        ErrorResponse body = ErrorResponse.of(
                CommonErrorCode.ACCESS_DENIED,
                List.of(),
                TraceContext.traceId(request)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
