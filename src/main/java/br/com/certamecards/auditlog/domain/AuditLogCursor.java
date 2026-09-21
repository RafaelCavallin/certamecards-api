package br.com.certamecards.auditlog.domain;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;

public record AuditLogCursor(Instant createdAt, UUID id) {

    public static AuditLogCursor decode(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("\\|", 2);
        try {
            return new AuditLogCursor(Instant.ofEpochMilli(Long.parseLong(parts[0])), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }

    public String encode() {
        return createdAt.toEpochMilli() + "|" + id;
    }
}
