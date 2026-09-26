package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.util.RawValue;

public record ConflictDetail(
        UUID id,
        String entityType,
        UUID entityId,
        UUID deckId,
        String reason,
        RawValue losingSnapshot,
        RawValue winningSnapshot,
        Integer currentVersion,
        boolean currentDeleted,
        Instant expiresAt,
        Instant restoredAt) {

    public static ConflictDetail of(SyncConflict conflict, Integer currentVersion, boolean currentDeleted) {
        return new ConflictDetail(
                conflict.id(),
                conflict.entityType(),
                conflict.entityId(),
                conflict.deckId(),
                conflict.reason(),
                conflict.losingSnapshot(),
                conflict.winningSnapshot(),
                currentVersion,
                currentDeleted,
                conflict.expiresAt(),
                conflict.restoredAt());
    }
}
