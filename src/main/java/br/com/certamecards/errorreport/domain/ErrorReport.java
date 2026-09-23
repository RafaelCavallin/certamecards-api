package br.com.certamecards.errorreport.domain;

import java.time.Instant;
import java.util.UUID;

public record ErrorReport(
        UUID id, UUID cardId, ErrorReportReason reason, String note, ErrorReportStatus status, Instant createdAt) {}
