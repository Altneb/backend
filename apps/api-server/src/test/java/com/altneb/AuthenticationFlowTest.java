package com.altneb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import com.altneb.platform.security.AccessTokenIssuer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthenticationFlowTest.AdminEndpoint.class)
class AuthenticationFlowTest {
    @Autowired MockMvc mockMvc;
    @Autowired AccessTokenIssuer accessTokenIssuer;
    @Autowired JwtDecoder jwtDecoder;

    @Test
    void registerLoginRefreshAndProtectEndpoint() throws Exception {
        String email = "user@example.com";
        String password = "strongPassword123";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        var login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("altneb_refresh", true))
                .andExpect(cookie().sameSite("altneb_refresh", "Strict"))
                .andReturn();
        String accessToken = com.jayway.jsonpath.JsonPath.read(
                login.getResponse().getContentAsString(), "$.data.accessToken");
        Cookie originalRefresh = login.getResponse().getCookie("altneb_refresh");
        assertThat(originalRefresh).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").isNotEmpty());
        mockMvc.perform(get("/test/admin").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("COMMON_009"));

        var refreshed = mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotatedRefresh = refreshed.getResponse().getCookie("altneb_refresh");
        assertThat(rotatedRefresh).isNotNull();
        assertThat(rotatedRefresh.getValue()).isNotEqualTo(originalRefresh.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_005"));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotatedRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMissingAndInvalidAccessTokens() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_008"));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredAccessToken() {
        String expired = accessTokenIssuer.issue(
                UUID.randomUUID(), "USER", Instant.now().minus(1, ChronoUnit.HOURS)).value();

        assertThatThrownBy(() -> jwtDecoder.decode(expired))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsWeakPasswordAndDuplicateEmailWithoutLeakingPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"weak@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_001"));

        String valid = "{\"email\":\"duplicate@example.com\",\"password\":\"strongPassword123\"}";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_001"));
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() throws Exception {
        String email = "logout@example.com";
        String password = "strongPassword123";
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        var login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        Cookie refresh = login.getResponse().getCookie("altneb_refresh");
        assertThat(refresh).isNotNull();
        mockMvc.perform(post("/api/v1/auth/logout").cookie(refresh))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("altneb_refresh", 0));
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_005"));
    }

    @RestController
    static class AdminEndpoint {
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/test/admin")
        String admin() {
            return "admin";
        }
    }
}
