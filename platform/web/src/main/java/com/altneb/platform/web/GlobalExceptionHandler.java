package com.altneb.platform.web;

import com.altneb.platform.error.BusinessException;
import com.altneb.platform.error.CommonErrorCode;
import com.altneb.platform.error.ErrorCode;
import com.altneb.platform.error.ErrorDetail;
import com.altneb.platform.observability.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        return response(exception.errorCode(), exception.details(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new ErrorDetail(error.getField(), safeReason(error)))
                .toList();
        return response(CommonErrorCode.INVALID_INPUT, details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ErrorDetail> details = exception.getConstraintViolations().stream()
                .map(violation -> new ErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .sorted(Comparator.comparing(ErrorDetail::field))
                .toList();
        return response(CommonErrorCode.INVALID_INPUT, details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(CommonErrorCode.MALFORMED_REQUEST, List.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return response(CommonErrorCode.RESOURCE_NOT_FOUND, List.of(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return response(CommonErrorCode.METHOD_NOT_ALLOWED, List.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return response(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        String traceId = TraceContext.traceId(request);
        log.error("Unexpected request processing failure: traceId={}", traceId, exception);
        return response(CommonErrorCode.INTERNAL_SERVER_ERROR, List.of(), request);
    }

    private ResponseEntity<ErrorResponse> response(
            ErrorCode errorCode,
            List<ErrorDetail> details,
            HttpServletRequest request
    ) {
        HttpStatus status = ErrorTypeHttpStatusMapper.toHttpStatus(errorCode.type());
        ErrorResponse body = ErrorResponse.of(errorCode, details, TraceContext.traceId(request));
        return ResponseEntity.status(status).body(body);
    }

    private String safeReason(FieldError error) {
        return error.getDefaultMessage() == null ? "유효하지 않은 값입니다." : error.getDefaultMessage();
    }
}
