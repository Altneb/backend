package com.altneb.identity.application;

import com.altneb.identity.domain.RefreshSession;
import com.altneb.identity.domain.UserAccount;
import com.altneb.identity.error.IdentityErrorCode;
import com.altneb.identity.infrastructure.RefreshSessionRepository;
import com.altneb.identity.infrastructure.UserAccountRepository;
import com.altneb.platform.error.BusinessException;
import com.altneb.platform.security.AccessTokenIssuer;
import com.altneb.platform.security.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokens;
    private final SecurityProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public IdentityService(UserAccountRepository users, RefreshSessionRepository sessions,
                           PasswordEncoder passwordEncoder, AccessTokenIssuer accessTokens,
                           SecurityProperties properties, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public UUID register(String rawEmail, String password) {
        String email = normalizeEmail(rawEmail);
        if (users.existsByEmail(email)) {
            throw new BusinessException(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
        }
        UserAccount user = new UserAccount(UUID.randomUUID(), email, passwordEncoder.encode(password), "USER", clock.instant());
        try {
            return users.saveAndFlush(user).id();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(IdentityErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Transactional
    public AuthTokens login(String rawEmail, String password) {
        UserAccount user = users.findByEmail(normalizeEmail(rawEmail))
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new BusinessException(IdentityErrorCode.INVALID_CREDENTIALS);
        }
        return issue(user, UUID.randomUUID(), null);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthTokens refresh(String rawToken) {
        Instant now = clock.instant();
        RefreshSession current = sessions.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException(IdentityErrorCode.INVALID_REFRESH_TOKEN));
        if (current.revoked()) {
            sessions.revokeFamily(current.familyId(), now);
            throw new BusinessException(IdentityErrorCode.REFRESH_TOKEN_REUSED);
        }
        if (!current.expiresAt().isAfter(now)) {
            current.revoke(now, null);
            throw new BusinessException(IdentityErrorCode.EXPIRED_REFRESH_TOKEN);
        }
        return issue(current.user(), current.familyId(), current);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessions.findByTokenHash(hash(rawToken)).ifPresent(session -> {
            if (!session.revoked()) {
                session.revoke(clock.instant(), null);
            }
        });
    }

    private AuthTokens issue(UserAccount user, UUID familyId, RefreshSession previous) {
        Instant now = clock.instant();
        String refreshToken = newRefreshToken();
        RefreshSession next = new RefreshSession(UUID.randomUUID(), user, familyId, hash(refreshToken),
                now.plus(properties.jwt().refreshTokenTtl()), now);
        sessions.save(next);
        if (previous != null) {
            previous.revoke(now, next.id());
        }
        AccessTokenIssuer.IssuedAccessToken access = accessTokens.issue(user.id(), user.role(), now);
        return new AuthTokens(access.value(), access.expiresAt(), refreshToken);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
}
