package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.sync.persistence.ConflictPurgeQuery;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ConflictPurgeWorkerTest {

    private static final Instant NOW = Instant.parse("2026-09-22T03:15:00Z");
    private final ConflictPurgeQuery purgeQuery = mock(ConflictPurgeQuery.class);
    private final ConflictPurgeWorker worker = new ConflictPurgeWorker(purgeQuery, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void givenDueConflicts_whenRunning_thenExpiresThemAtCurrentInstant() {
        when(purgeQuery.expireDueConflicts(NOW)).thenReturn(3);

        int expired = worker.expireDueConflicts();

        assertThat(expired).isEqualTo(3);
        verify(purgeQuery).expireDueConflicts(NOW);
    }
}
