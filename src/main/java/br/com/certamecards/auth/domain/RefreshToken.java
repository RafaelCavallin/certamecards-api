package br.com.certamecards.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by")
    private UUID replacedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "user_agent")
    private String userAgent;

    protected RefreshToken() {}

    public RefreshToken(UUID id, UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public RefreshToken(UUID id, UUID userId, UUID familyId, String tokenHash, Instant expiresAt, Instant createdAt) {
        this(id, userId, familyId, tokenHash, expiresAt);
        this.createdAt = createdAt;
    }

    @PrePersist
    private void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void revoke(Instant now) {
        this.revokedAt = now;
    }

    public void replaceBy(UUID nextTokenId) {
        this.replacedBy = nextTokenId;
    }

    public void assignUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedBy() {
        return replacedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
