package br.com.certamecards.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ContentAuditTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Test
    void givenNewInstance_whenReadingTimestamps_thenAllAreNull() {
        ContentAudit audit = new ContentAudit();
        assertThat(audit.getCreatedAt()).isNull();
        assertThat(audit.getUpdatedAt()).isNull();
        assertThat(audit.getDeletedAt()).isNull();
    }

    @Test
    void givenNewInstance_whenInitializing_thenCreatedAndUpdatedMatchGivenInstant() {
        ContentAudit audit = new ContentAudit();
        audit.initialize(NOW);
        assertThat(audit.getCreatedAt()).isEqualTo(NOW);
        assertThat(audit.getUpdatedAt()).isEqualTo(NOW);
        assertThat(audit.getDeletedAt()).isNull();
    }

    @Test
    void givenInitializedAudit_whenTouching_thenOnlyUpdatedAtChanges() {
        ContentAudit audit = new ContentAudit();
        audit.initialize(NOW);
        Instant later = NOW.plusSeconds(60);
        audit.touch(later);
        assertThat(audit.getCreatedAt()).isEqualTo(NOW);
        assertThat(audit.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void givenInitializedAudit_whenMarkingDeleted_thenDeletedAndUpdatedMatchGivenInstant() {
        ContentAudit audit = new ContentAudit();
        audit.initialize(NOW);
        Instant later = NOW.plusSeconds(60);
        audit.markDeleted(later);
        assertThat(audit.getDeletedAt()).isEqualTo(later);
        assertThat(audit.getUpdatedAt()).isEqualTo(later);
    }
}
