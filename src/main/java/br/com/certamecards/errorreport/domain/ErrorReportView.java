package br.com.certamecards.errorreport.domain;

import java.time.Instant;
import java.util.UUID;

public record ErrorReportView(
        UUID id,
        UUID cardId,
        UUID deckId,
        String deckName,
        String subjectName,
        String cardFront,
        String reporterName,
        String reason,
        String note,
        String status,
        Instant createdAt,
        Instant closedAt,
        UUID closedBy) {}
