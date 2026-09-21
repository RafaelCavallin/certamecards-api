package br.com.certamecards.officialdeck.domain;

import java.time.Instant;
import java.util.UUID;

public record OfficialDeckAdminSummary(
        UUID id,
        UUID subjectId,
        String subjectName,
        String name,
        String description,
        String status,
        int cardCount,
        int subscriberCount,
        int openReportCount,
        Instant contentUpdatedAt,
        int version) {}
