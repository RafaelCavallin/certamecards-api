package br.com.certamecards.auditlog.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntry(
        UUID id,
        UUID actorId,
        String actorName,
        String action,
        String targetType,
        UUID targetId,
        String targetLabel,
        String changes,
        Instant createdAt) {}
