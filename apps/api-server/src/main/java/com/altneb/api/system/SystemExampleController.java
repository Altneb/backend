package com.altneb.api.system;

import com.altneb.platform.observability.TraceContext;
import com.altneb.platform.web.ApiResponse;
import com.altneb.platform.web.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "System Example", description = "API 공통 계약 검증용 Endpoint")
@RestController
@RequestMapping("/api/v1/system/examples")
public class SystemExampleController {

    @Operation(
            summary = "공통 성공 응답 형식 확인",
            description = "인증이 필요하지 않은 API 계약 검증용 Endpoint입니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "정상 응답",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "입력값 검증 실패",
            content = @Content(
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "success": false,
                              "error": {
                                "code": "COMMON_001",
                                "message": "입력값이 올바르지 않습니다.",
                                "details": [{"field": "message", "reason": "message는 필수입니다."}]
                              },
                              "traceId": "example-trace-id"
                            }
                            """)
            )
    )
    @PostMapping("/echo")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<EchoResponse> echo(
            @Valid @RequestBody EchoRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                new EchoResponse(request.message()),
                TraceContext.traceId(servletRequest)
        );
    }

    public record EchoRequest(
            @NotBlank(message = "message는 필수입니다.")
            @Size(max = 100, message = "message는 100자 이하여야 합니다.")
            String message
    ) {
    }

    public record EchoResponse(String message) {
    }
}
