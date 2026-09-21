package br.com.certamecards.auditlog.web;

import br.com.certamecards.auditlog.domain.AuditLogEntry;
import java.time.Instant;
import tools.jackson.databind.util.RawValue;

public record AuditLogEntryResponse(
        String id,
        String actorId,
        String actorName,
        String action,
        String targetType,
        String targetId,
        String targetLabel,
        RawValue changes,
        Instant createdAt) {

    public static AuditLogEntryResponse from(AuditLogEntry entry) {
        return new AuditLogEntryResponse(
                entry.id().toString(),
                entry.actorId().toString(),
                entry.actorName(),
                entry.action(),
                entry.targetType(),
                entry.targetId() == null ? null : entry.targetId().toString(),
                entry.targetLabel(),
                new RawValue(entry.changes()),
                entry.createdAt());
    }
}
