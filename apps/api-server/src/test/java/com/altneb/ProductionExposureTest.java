package com.altneb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionExposureTest {

    @Autowired
    private Environment environment;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void disablesApiDocsAndBindsManagementServerToLoopbackByDefault() throws Exception {
        assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.server.address")).isEqualTo("127.0.0.1");

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMON_003"));
    }
}
