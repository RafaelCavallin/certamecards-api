package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.persistence.DeckSubscriptionStore;
import br.com.certamecards.review.persistence.CardStateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class DeckContentServiceTest {

    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final DeckSubscriptionStore store = mock(DeckSubscriptionStore.class);
    private final CardRepository cardRepository = mock(CardRepository.class);
    private final CardStateRepository stateRepository = mock(CardStateRepository.class);
    private final DeckContentService service =
            new DeckContentService(deckAccess, store, cardRepository, stateRepository);
    private final UUID userId = UUID.randomUUID();
    private final UUID deckId = UUID.randomUUID();

    @Test
    void givenNoActiveSubscription_whenReadingContent_thenThrowsNotSubscribed() {
        when(store.find(userId, deckId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.content(userId, deckId, new ContentCursor(null, 2)))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_SUBSCRIBED));
    }

    @Test
    void givenMoreCardsThanTheLimit_whenReadingContent_thenReturnsFullPageWithCursor() {
        activeSubscription();
        List<Card> fetched = List.of(card(), card(), card());
        when(cardRepository.findByDeckIdAndAudit_DeletedAtIsNullAndIdGreaterThanOrderById(
                        eq(deckId), any(UUID.class), any(Pageable.class)))
                .thenReturn(fetched);
        when(stateRepository.findByIdUserIdAndIdCardIdIn(eq(userId), any())).thenReturn(List.of());

        DeckContentPage page = service.content(userId, deckId, new ContentCursor(null, 2));

        assertThat(page.cards()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextAfter()).isEqualTo(fetched.get(1).getId());
    }

    @Test
    void givenFewerCardsThanTheLimit_whenReadingContent_thenReturnsLastPageWithoutCursor() {
        activeSubscription();
        UUID after = UUID.randomUUID();
        when(cardRepository.findByDeckIdAndAudit_DeletedAtIsNullAndIdGreaterThanOrderById(
                        eq(deckId), eq(after), any(Pageable.class)))
                .thenReturn(List.of(card()));
        when(stateRepository.findByIdUserIdAndIdCardIdIn(eq(userId), any())).thenReturn(List.of());

        DeckContentPage page = service.content(userId, deckId, new ContentCursor(after, 2));

        assertThat(page.cards()).hasSize(1);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextAfter()).isNull();
    }

    private void activeSubscription() {
        when(store.find(userId, deckId)).thenReturn(Optional.of(new DeckSubscription(deckId, Instant.EPOCH, null, 1)));
    }

    private Card card() {
        return new Card(UUID.randomUUID(), deckId, "frente", "verso");
    }
}
