package com.altneb.platform.security;

import com.altneb.platform.error.CommonErrorCode;
import com.altneb.platform.observability.TraceContext;
import com.altneb.platform.web.ErrorResponse;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            JwtAuthenticationConverter authenticationConverter
    ) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // Versioned APIs authenticate with Bearer tokens. The only credential in a cookie is the
        // Strict SameSite refresh token, scoped to the authentication path.
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**"));
        http.cors(Customizer.withDefaults());
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/api/v1/auth/register", "/api/v1/auth/login",
                        "/api/v1/auth/refresh", "/api/v1/auth/logout",
                        "/api/v1/system/examples/**", "/test/**", "/v3/api-docs/**",
                        "/swagger-ui/**", "/swagger-ui.html", "/actuator/health",
                        "/actuator/prometheus")
                .permitAll()
                .requestMatchers("/api/v1/auth/me").authenticated()
                .anyRequest().permitAll());
        http.oauth2ResourceServer(resource -> resource
                .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
                .authenticationEntryPoint((request, response, exception) -> writeError(
                        response, objectMapper, CommonErrorCode.UNAUTHENTICATED,
                        TraceContext.traceId(request), HttpServletResponse.SC_UNAUTHORIZED)));
        http.exceptionHandling(exceptions -> exceptions.accessDeniedHandler(
                (request, response, exception) -> writeError(
                        response, objectMapper, CommonErrorCode.ACCESS_DENIED,
                        TraceContext.traceId(request), HttpServletResponse.SC_FORBIDDEN)));
        return http.build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties.jwt())));
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        SecurityProperties.Jwt jwt = properties.jwt();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(jwt)).build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(jwt.issuer()),
                new JwtAudienceValidator(jwt.audience()));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    AccessTokenIssuer accessTokenIssuer(JwtEncoder encoder, SecurityProperties properties) {
        return new JwtAccessTokenService(encoder, properties.jwt());
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private SecretKey secretKey(SecurityProperties.Jwt properties) {
        String encoded = properties.secretBase64();
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be configured");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encoded.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be valid Base64", exception);
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 bytes");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    private void writeError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            CommonErrorCode errorCode,
            String traceId,
            int status
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode, List.of(), traceId));
    }
}
