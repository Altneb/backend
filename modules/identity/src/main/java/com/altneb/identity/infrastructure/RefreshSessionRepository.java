package com.altneb.identity.infrastructure;

import com.altneb.identity.domain.RefreshSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("update RefreshSession r set r.revokedAt = :at where r.familyId = :familyId and r.revokedAt is null")
    int revokeFamily(UUID familyId, Instant at);
}
