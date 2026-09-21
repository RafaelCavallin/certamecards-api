package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record CardChange(
        UUID id,
        UUID deckId,
        String type,
        String front,
        String back,
        String source,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        int version,
        long changeSeq) {}
