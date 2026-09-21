package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OneTimeTokenTest {

    @Test
    void givenIdUserPurposeHashAndExpiry_whenConstructing_thenGettersReturnValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-09-18T12:00:00Z");
        OneTimeToken token = new OneTimeToken(id, userId, OneTimeTokenPurpose.CONFIRM_EMAIL, "hash", expiresAt);
        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getPurpose()).isEqualTo(OneTimeTokenPurpose.CONFIRM_EMAIL);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getPayload()).isNull();
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void givenToken_whenMarkingUsed_thenUsedAtIsSet() {
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OneTimeTokenPurpose.RESET_PASSWORD,
                "hash",
                Instant.parse("2026-09-18T12:00:00Z"));
        Instant now = Instant.parse("2026-09-17T12:00:00Z");
        token.markUsed(now);
        assertThat(token.getUsedAt()).isEqualTo(now);
    }
}
