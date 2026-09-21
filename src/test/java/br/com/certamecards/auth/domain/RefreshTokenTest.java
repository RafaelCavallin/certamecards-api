package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private final UUID id = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();
    private final Instant expiresAt = Instant.parse("2026-12-17T12:00:00Z");

    @Test
    void givenIdUserFamilyHashAndExpiry_whenConstructing_thenGettersReturnValues() {
        RefreshToken token = new RefreshToken(id, userId, familyId, "hash", expiresAt);
        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getFamilyId()).isEqualTo(familyId);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getRevokedAt()).isNull();
        assertThat(token.getReplacedBy()).isNull();
        assertThat(token.getCreatedAt()).isNull();
        assertThat(token.getUserAgent()).isNull();
    }

    @Test
    void givenToken_whenRevoking_thenRevokedAtIsSet() {
        RefreshToken token = new RefreshToken(id, userId, familyId, "hash", expiresAt);
        Instant now = Instant.parse("2026-09-17T12:00:00Z");
        token.revoke(now);
        assertThat(token.getRevokedAt()).isEqualTo(now);
    }

    @Test
    void givenToken_whenReplacedBy_thenReplacedByIsSet() {
        RefreshToken token = new RefreshToken(id, userId, familyId, "hash", expiresAt);
        UUID nextId = UUID.randomUUID();
        token.replaceBy(nextId);
        assertThat(token.getReplacedBy()).isEqualTo(nextId);
    }

    @Test
    void givenToken_whenAssigningUserAgent_thenGetterReturnsIt() {
        RefreshToken token = new RefreshToken(id, userId, familyId, "hash", expiresAt);
        token.assignUserAgent("Mozilla/5.0");
        assertThat(token.getUserAgent()).isEqualTo("Mozilla/5.0");
    }

    @Test
    void givenCreatedAt_whenConstructing_thenGetterReturnsProvidedInstant() {
        Instant createdAt = Instant.parse("2026-09-17T12:00:00Z");
        RefreshToken token = new RefreshToken(id, userId, familyId, "hash", expiresAt, createdAt);
        assertThat(token.getCreatedAt()).isEqualTo(createdAt);
    }
}
