package com.altneb.identity.api;

import java.time.Instant;

public record AccessTokenResponse(String accessToken, String tokenType, Instant expiresAt) {
}
