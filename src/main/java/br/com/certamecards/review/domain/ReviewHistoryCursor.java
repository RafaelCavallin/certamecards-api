package br.com.certamecards.review.domain;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record ReviewHistoryCursor(Instant eventAt, int eventCounter, UUID eventDeviceId, UUID operationId) {

    private static final String DELIMITER = "|";

    public String encode() {
        String raw = eventAt + DELIMITER + eventCounter + DELIMITER + eventDeviceId + DELIMITER + operationId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static ReviewHistoryCursor decode(String opaque) {
        String raw = new String(Base64.getUrlDecoder().decode(opaque), StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Malformed review history cursor");
        }
        Instant eventAt = Instant.parse(parts[0]);
        int eventCounter = Integer.parseInt(parts[1]);
        UUID eventDeviceId = UUID.fromString(parts[2]);
        UUID operationId = UUID.fromString(parts[3]);
        return new ReviewHistoryCursor(eventAt, eventCounter, eventDeviceId, operationId);
    }
}
