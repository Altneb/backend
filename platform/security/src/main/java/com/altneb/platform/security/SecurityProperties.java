package com.altneb.platform.security;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("altneb.security")
public record SecurityProperties(
        Jwt jwt,
        Cors cors,
        RefreshCookie refreshCookie
) {
    public record Jwt(
            String issuer,
            String audience,
            String secretBase64,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record RefreshCookie(boolean secure) {
    }
}
