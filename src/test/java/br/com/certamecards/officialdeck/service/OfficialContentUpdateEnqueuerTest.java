package br.com.certamecards.officialdeck.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialContentUpdateEnqueuerTest {

    @Test
    void givenChangedCard_whenEnqueuing_thenStoresJobWithNoteAndEditInstant() {
        ContentUpdateJobStore store = mock(ContentUpdateJobStore.class);
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "F", "B");
        Instant now = Instant.parse("2026-09-19T14:05:00Z");

        new OfficialContentUpdateEnqueuer(store).enqueue(card, "Lei nova", now);

        verify(store).enqueue(new ContentUpdateJob(null, card.getId(), card.getDeckId(), "Lei nova", now, null));
    }
}
