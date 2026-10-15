package com.altneb.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.altneb.platform.error.ErrorType;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

class ErrorTypeHttpStatusMapperTest {

    @ParameterizedTest
    @MethodSource("statusMappings")
    void mapsTransportIndependentErrorTypeToHttpStatus(ErrorType errorType, HttpStatus status) {
        assertThat(ErrorTypeHttpStatusMapper.toHttpStatus(errorType)).isEqualTo(status);
    }

    private static Stream<Arguments> statusMappings() {
        return Stream.of(
                Arguments.of(ErrorType.VALIDATION, HttpStatus.BAD_REQUEST),
                Arguments.of(ErrorType.AUTHENTICATION, HttpStatus.UNAUTHORIZED),
                Arguments.of(ErrorType.AUTHORIZATION, HttpStatus.FORBIDDEN),
                Arguments.of(ErrorType.NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(ErrorType.OPERATION_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED),
                Arguments.of(ErrorType.UNSUPPORTED_CONTENT_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE),
                Arguments.of(ErrorType.CONFLICT, HttpStatus.CONFLICT),
                Arguments.of(ErrorType.UNPROCESSABLE, HttpStatus.UNPROCESSABLE_CONTENT),
                Arguments.of(ErrorType.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS),
                Arguments.of(ErrorType.DEPENDENCY_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
                Arguments.of(ErrorType.INTERNAL, HttpStatus.INTERNAL_SERVER_ERROR)
        );
    }
}
