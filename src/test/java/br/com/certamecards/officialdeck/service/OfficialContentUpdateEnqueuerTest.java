package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialContentUpdateEnqueuerTest {

    @Test
    void givenChangedCard_whenEnqueuing_thenStoresJobWithNoteAndEditInstant() {
        ContentUpdateJobStore store = mock(ContentUpdateJobStore.class);
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "F", "B");
        Instant now = Instant.parse("2026-09-19T14:05:00Z");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        new OfficialContentUpdateEnqueuer(store, new OfficialMetrics(registry)).enqueue(card, "Lei nova", now);

        assertThat(registry.counter("official.content_update.jobs", "result", "queued")
                        .count())
                .isEqualTo(1);

        verify(store).enqueue(new ContentUpdateJob(null, card.getId(), card.getDeckId(), "Lei nova", now, null));
    }
}
