package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record DeckChange(
        UUID id,
        UUID subjectId,
        String name,
        String description,
        String origin,
        UUID originRef,
        String officialStatus,
        String originLabel,
        Instant contentUpdatedAt,
        int cardCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        int version,
        long changeSeq) {}
