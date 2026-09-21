package br.com.certamecards.auditlog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogCursorTest {

    @Test
    void givenNull_whenDecoding_thenReturnsNull() {
        assertThat(AuditLogCursor.decode(null)).isNull();
    }

    @Test
    void givenEncodedCursor_whenDecoding_thenRestoresCreatedAtAndId() {
        Instant createdAt = Instant.parse("2026-09-19T14:05:00Z");
        UUID id = UUID.randomUUID();
        AuditLogCursor original = new AuditLogCursor(createdAt, id);

        AuditLogCursor decoded = AuditLogCursor.decode(original.encode());

        assertThat(decoded.createdAt()).isEqualTo(createdAt);
        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void givenMalformedCursor_whenDecoding_thenThrowsValidationFailed() {
        assertThatThrownBy(() -> AuditLogCursor.decode("not-a-cursor"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
