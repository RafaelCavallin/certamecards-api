package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.library.domain.DeckPreview;
import br.com.certamecards.library.domain.LibraryDeckSummary;
import br.com.certamecards.library.domain.PreviewCard;
import br.com.certamecards.library.persistence.LibraryDeckLookupQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeckPreviewServiceTest {

    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final LibraryDeckLookupQuery lookupQuery = mock(LibraryDeckLookupQuery.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final DeckPreviewService service = new DeckPreviewService(
            deckAccess,
            new DeckAvailabilityGuard(),
            lookupQuery,
            new LibraryProperties(90, 10, 5, 20, 500, 3),
            new LibraryMetrics(registry));
    private final UUID userId = UUID.randomUUID();
    private final Deck deck =
            new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);

    @Test
    void givenPublishedDeck_whenPreviewing_thenReturnsSummaryAndConfiguredNumberOfCards() {
        deck.getOfficialMeta().changeStatus("published");
        LibraryDeckSummary summary = new LibraryDeckSummary(
                deck.getId(), deck.getSubjectId(), "Direito", "CF/88", null, 12, Instant.EPOCH, false);
        List<PreviewCard> cards = List.of(new PreviewCard(UUID.randomUUID(), "f", "b", null));
        when(deckAccess.findOfficial(deck.getId())).thenReturn(deck);
        when(lookupQuery.findSummary(userId, deck.getId())).thenReturn(Optional.of(summary));
        when(lookupQuery.previewCards(deck.getId(), 10)).thenReturn(cards);

        DeckPreview preview = service.preview(userId, deck.getId());

        assertThat(preview.deck()).isEqualTo(summary);
        assertThat(preview.cards()).isEqualTo(cards);
    }

    @Test
    void givenDraftDeck_whenPreviewing_thenThrowsDeckNotAvailable() {
        deck.getOfficialMeta().changeStatus("draft");
        when(deckAccess.findOfficial(deck.getId())).thenReturn(deck);

        assertThatThrownBy(() -> service.preview(userId, deck.getId()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.DECK_NOT_AVAILABLE));
    }

    @Test
    void givenSummaryMissing_whenPreviewing_thenThrowsNotFound() {
        deck.getOfficialMeta().changeStatus("published");
        when(deckAccess.findOfficial(deck.getId())).thenReturn(deck);
        when(lookupQuery.findSummary(userId, deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(userId, deck.getId()))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenPublishedDeck_whenPreviewing_thenCountsThePreview() {
        deck.getOfficialMeta().changeStatus("published");
        when(deckAccess.findOfficial(deck.getId())).thenReturn(deck);
        when(lookupQuery.findSummary(userId, deck.getId()))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(LibraryDeckSummary.class)));

        service.preview(userId, deck.getId());

        assertThat(registry.counter("library.preview").count()).isEqualTo(1);
    }
}
