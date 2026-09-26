package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.util.RawValue;

public record SyncConflict(
        UUID id,
        UUID userId,
        String entityType,
        UUID entityId,
        UUID deckId,
        UUID losingOperationId,
        UUID winningOperationId,
        String reason,
        RawValue losingSnapshot,
        RawValue winningSnapshot,
        Instant expiresAt,
        Instant restoredAt,
        Instant expiredAt,
        Instant createdAt) {

    public boolean isExpired(Instant now) {
        return expiredAt != null || now.isAfter(expiresAt);
    }
}
