package com.altneb.identity.api;

import com.altneb.identity.application.AuthTokens;
import com.altneb.identity.application.IdentityService;
import com.altneb.platform.security.SecurityProperties;
import com.altneb.platform.observability.TraceContext;
import com.altneb.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class IdentityController {
    private static final String REFRESH_COOKIE = "altneb_refresh";
    private final IdentityService identityService;
    private final SecurityProperties properties;

    public IdentityController(IdentityService identityService, SecurityProperties properties) {
        this.identityService = identityService;
        this.properties = properties;
    }

    @Operation(summary = "회원가입")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가입 완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 중복")
    })
    @PostMapping("/register")
    ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        var response = new RegisterResponse(identityService.register(request.email(), request.password()));
        return ResponseEntity.status(201).body(ApiResponse.success(response, TraceContext.traceId(servletRequest)));
    }

    @Operation(summary = "로그인")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/login")
    ResponseEntity<ApiResponse<AccessTokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return tokenResponse(identityService.login(request.email(), request.password()), servletRequest);
    }

    @Operation(summary = "Access Token 갱신")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token 갱신 완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 오류")
    })
    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest servletRequest) {
        return tokenResponse(identityService.refresh(refreshToken), servletRequest);
    }

    @Operation(summary = "로그아웃")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 완료")
    })
    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        identityService.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredCookie().toString()).build();
    }

    @Operation(summary = "현재 인증 사용자 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 조회 완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/me")
    ApiResponse<CurrentUserResponse> me(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return ApiResponse.success(new CurrentUserResponse(jwt.getSubject()), TraceContext.traceId(request));
    }

    private ResponseEntity<ApiResponse<AccessTokenResponse>> tokenResponse(
            AuthTokens tokens, HttpServletRequest request) {
        var body = new AccessTokenResponse(tokens.accessToken(), "Bearer", tokens.accessTokenExpiresAt());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success(body, TraceContext.traceId(request)));
    }

    private ResponseCookie refreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true).secure(properties.refreshCookie().secure()).sameSite("Strict")
                .path("/api/v1/auth").maxAge(properties.jwt().refreshTokenTtl()).build();
    }

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(properties.refreshCookie().secure()).sameSite("Strict")
                .path("/api/v1/auth").maxAge(Duration.ZERO).build();
    }
}
