package br.com.certamecards.errorreport.web;

import br.com.certamecards.errorreport.domain.ErrorReportView;
import java.time.Instant;
import java.util.UUID;

public record ErrorReportAdminResponse(
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
        UUID closedBy) {

    static ErrorReportAdminResponse from(ErrorReportView view) {
        return new ErrorReportAdminResponse(
                view.id(),
                view.cardId(),
                view.deckId(),
                view.deckName(),
                view.subjectName(),
                view.cardFront(),
                view.reporterName(),
                view.reason(),
                view.note(),
                view.status(),
                view.createdAt(),
                view.closedAt(),
                view.closedBy());
    }
}
