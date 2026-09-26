package br.com.certamecards.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConflictCursorTest {

    @Test
    void givenCursor_whenEncodingAndDecoding_thenRoundTrips() {
        Instant expiresAt = Instant.parse("2026-10-01T12:00:00Z");
        UUID id = UUID.randomUUID();
        ConflictCursor cursor = new ConflictCursor(expiresAt, id);

        ConflictCursor decoded = ConflictCursor.decode(cursor.encode());

        assertThat(decoded).isEqualTo(cursor);
    }

    @Test
    void givenMalformedOpaqueString_whenDecoding_thenThrowsIllegalArgument() {
        String malformed =
                java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("not-a-valid-cursor".getBytes());

        assertThatThrownBy(() -> ConflictCursor.decode(malformed)).isInstanceOf(IllegalArgumentException.class);
    }
}
