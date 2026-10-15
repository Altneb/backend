package com.altneb;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.altneb.platform.error.BusinessException;
import com.altneb.platform.error.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebContractTest.ErrorScenarioController.class)
class WebContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSuccessEnvelopeAndPropagatesSafeTraceId() throws Exception {
        mockMvc.perform(post("/api/v1/system/examples/echo")
                        .header("X-Trace-Id", "contract-test-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"hello"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "contract-test-123"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("hello"))
                .andExpect(jsonPath("$.traceId").value("contract-test-123"));
    }

    @Test
    void rejectsInvalidInputWithStableErrorContract() throws Exception {
        mockMvc.perform(post("/api/v1/system/examples/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_001"))
                .andExpect(jsonPath("$.error.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.error.details[0].field").value("message"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void mapsBusinessExceptionToItsHttpStatus() throws Exception {
        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COMMON_006"));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("COMMON_007"))
                .andExpect(jsonPath("$.error.message").value("요청 처리 중 오류가 발생했습니다."))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("sensitive SQL detail")
                )));
    }

    @Test
    void returnsSafeNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMON_003"));
    }

    @Test
    void returnsMethodNotAllowedWithStableErrorContract() throws Exception {
        mockMvc.perform(get("/api/v1/system/examples/echo"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("COMMON_004"));
    }

    @Test
    void exposesOpenApiHealthAndPrometheusInLocalProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Altneb Backend API"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @RestController
    @RequestMapping("/test/errors")
    static class ErrorScenarioController {

        @GetMapping("/business")
        void businessError() {
            throw new BusinessException(CommonErrorCode.CONFLICT);
        }

        @GetMapping("/unexpected")
        void unexpectedError() {
            throw new IllegalStateException("sensitive SQL detail");
        }
    }
}
