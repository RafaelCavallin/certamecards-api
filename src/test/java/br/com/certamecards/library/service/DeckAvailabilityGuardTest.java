package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeckAvailabilityGuardTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:00:00Z");

    private final DeckAvailabilityGuard guard = new DeckAvailabilityGuard();

    @Test
    void givenPublishedDeck_whenEnsuring_thenPasses() {
        assertThatCode(() -> guard.ensurePublished(deckWithStatus("published"))).doesNotThrowAnyException();
    }

    @Test
    void givenDraftOrDiscontinuedDeck_whenEnsuring_thenThrowsDeckNotAvailable() {
        for (String status : new String[] {"draft", "discontinued"}) {
            assertThatThrownBy(() -> guard.ensurePublished(deckWithStatus(status)))
                    .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrorCode())
                            .isEqualTo(ErrorCode.DECK_NOT_AVAILABLE));
        }
    }

    @Test
    void givenDeletedDeck_whenEnsuring_thenThrowsNotFound() {
        Deck deck = deckWithStatus("published");
        deck.markDeleted(NOW);

        assertThatThrownBy(() -> guard.ensurePublished(deck))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private Deck deckWithStatus(String status) {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        deck.getOfficialMeta().changeStatus(status);
        return deck;
    }
}
