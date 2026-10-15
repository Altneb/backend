package com.altneb.platform.security;

import java.time.Instant;
import java.util.UUID;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(UUID userId, String role, Instant issuedAt);

    record IssuedAccessToken(String value, Instant expiresAt) {
    }
}
