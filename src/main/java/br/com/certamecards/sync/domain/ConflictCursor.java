package br.com.certamecards.sync.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record ConflictCursor(Instant expiresAt, UUID id) {

    private static final String DELIMITER = "|";

    public String encode() {
        String raw = expiresAt + DELIMITER + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ConflictCursor decode(String opaque) {
        String raw = new String(Base64.getUrlDecoder().decode(opaque), StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Malformed conflict cursor");
        }
        return new ConflictCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
    }
}
