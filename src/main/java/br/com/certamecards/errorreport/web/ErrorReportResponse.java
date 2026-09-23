package br.com.certamecards.errorreport.web;

import br.com.certamecards.errorreport.domain.ErrorReport;
import java.time.Instant;
import java.util.UUID;

public record ErrorReportResponse(UUID id, UUID cardId, String reason, String note, String status, Instant createdAt) {

    static ErrorReportResponse from(ErrorReport report) {
        return new ErrorReportResponse(
                report.id(),
                report.cardId(),
                report.reason().code(),
                report.note(),
                report.status().code(),
                report.createdAt());
    }
}
