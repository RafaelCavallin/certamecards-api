package br.com.certamecards.auditlog.domain;

import java.time.LocalDate;
import java.util.UUID;

public record AuditLogFilter(
        UUID actorId, AuditAction action, LocalDate from, LocalDate to, AuditLogCursor before, int size) {}
