package br.com.certamecards.library.domain;

import java.time.Instant;
import java.util.UUID;

public record LibraryDeckSummary(
        UUID id,
        UUID subjectId,
        String subjectName,
        String name,
        String description,
        int cardCount,
        Instant contentUpdatedAt,
        boolean subscribed) {}
