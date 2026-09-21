package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialDeckCardCountAdjusterTest {

    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final OfficialDeckCardCountAdjuster adjuster = new OfficialDeckCardCountAdjuster(deckAccess);
    private final Instant now = Instant.parse("2026-09-19T14:00:00Z");

    @Test
    void whenIncrementing_thenCardCountGrowsAndDeckIsSaved() {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);

        adjuster.increment(deck, now);

        assertThat(deck.getOfficialMeta().getCardCount()).isEqualTo(1);
        assertThat(deck.getOfficialMeta().getContentUpdatedAt()).isEqualTo(now);
        verify(deckAccess).save(deck);
    }

    @Test
    void whenDecrementing_thenCardCountShrinksAndDeckIsSaved() {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        deck.getOfficialMeta().incrementCardCount();

        adjuster.decrement(deck, now);

        assertThat(deck.getOfficialMeta().getCardCount()).isZero();
        verify(deckAccess).save(deck);
    }
}
