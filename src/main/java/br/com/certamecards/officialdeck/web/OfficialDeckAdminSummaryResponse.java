package br.com.certamecards.officialdeck.web;

import br.com.certamecards.officialdeck.domain.OfficialDeckAdminSummary;
import java.time.Instant;

public record OfficialDeckAdminSummaryResponse(
        String id,
        String subjectId,
        String subjectName,
        String name,
        String description,
        String status,
        int cardCount,
        int subscriberCount,
        int openReportCount,
        Instant contentUpdatedAt,
        int version) {

    public static OfficialDeckAdminSummaryResponse from(OfficialDeckAdminSummary summary) {
        return new OfficialDeckAdminSummaryResponse(
                summary.id().toString(),
                summary.subjectId().toString(),
                summary.subjectName(),
                summary.name(),
                summary.description(),
                summary.status(),
                summary.cardCount(),
                summary.subscriberCount(),
                summary.openReportCount(),
                summary.contentUpdatedAt(),
                summary.version());
    }
}
