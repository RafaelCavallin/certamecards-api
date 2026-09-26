package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record ConflictSummary(
        UUID id, String entityType, UUID entityId, UUID deckId, String reason, Instant expiresAt, Instant createdAt) {

    public static ConflictSummary from(SyncConflict conflict) {
        return new ConflictSummary(
                conflict.id(),
                conflict.entityType(),
                conflict.entityId(),
                conflict.deckId(),
                conflict.reason(),
                conflict.expiresAt(),
                conflict.createdAt());
    }
}
