package com.altneb.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "identity_refresh_sessions")
public class RefreshSession {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
    @Column(name = "family_id", nullable = false) private UUID familyId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "replaced_by_id") private UUID replacedById;

    protected RefreshSession() { }

    public RefreshSession(UUID id, UserAccount user, UUID familyId, String tokenHash,
                          Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.user = user;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public UUID id() { return id; }
    public UserAccount user() { return user; }
    public UUID familyId() { return familyId; }
    public String tokenHash() { return tokenHash; }
    public Instant expiresAt() { return expiresAt; }
    public boolean revoked() { return revokedAt != null; }
    public void revoke(Instant at, UUID replacementId) {
        revokedAt = at;
        replacedById = replacementId;
    }
}
