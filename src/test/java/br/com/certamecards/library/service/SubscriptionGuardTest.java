package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.CardLimitException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.domain.UserCardLimitCalculator;
import br.com.certamecards.library.persistence.UserCardCountsQuery;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionGuardTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:00:00Z");

    private final UserCardCountsQuery countsQuery = mock(UserCardCountsQuery.class);
    private final SubscriptionGuard guard =
            new SubscriptionGuard(new DeckAvailabilityGuard(), countsQuery, new UserCardLimitCalculator());
    private final UUID userId = UUID.randomUUID();

    @Test
    void givenPublishedDeckThatFits_whenEnsuring_thenPasses() {
        when(countsQuery.usedCards(userId)).thenReturn(100L);

        assertThatCode(() -> guard.ensureCanSubscribe(userId, deck("published", 12), Optional.empty()))
                .doesNotThrowAnyException();
    }

    @Test
    void givenCancelledPreviousSubscription_whenEnsuring_thenPasses() {
        when(countsQuery.usedCards(userId)).thenReturn(0L);
        DeckSubscription cancelled = new DeckSubscription(UUID.randomUUID(), NOW, NOW, 1);

        assertThatCode(() -> guard.ensureCanSubscribe(userId, deck("published", 12), Optional.of(cancelled)))
                .doesNotThrowAnyException();
    }

    @Test
    void givenActiveSubscription_whenEnsuring_thenThrowsAlreadySubscribed() {
        DeckSubscription active = new DeckSubscription(UUID.randomUUID(), NOW, null, 1);

        assertThatThrownBy(() -> guard.ensureCanSubscribe(userId, deck("published", 12), Optional.of(active)))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.ALREADY_SUBSCRIBED));
    }

    @Test
    void givenDraftDeck_whenEnsuring_thenThrowsDeckNotAvailable() {
        assertThatThrownBy(() -> guard.ensureCanSubscribe(userId, deck("draft", 12), Optional.empty()))
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ErrorCode.DECK_NOT_AVAILABLE));
    }

    @Test
    void givenDeckThatDoesNotFit_whenEnsuring_thenThrowsUserCardLimit() {
        when(countsQuery.usedCards(userId)).thenReturn(49_990L);

        assertThatThrownBy(() -> guard.ensureCanSubscribe(userId, deck("published", 30), Optional.empty()))
                .isInstanceOfSatisfying(CardLimitException.class, ex -> {
                    assertThat(ex.getRequiredCards()).isEqualTo(30);
                    assertThat(ex.getAvailableCards()).isEqualTo(10);
                });
    }

    private Deck deck(String status, int cardCount) {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        deck.getOfficialMeta().changeStatus(status);
        for (int index = 0; index < cardCount; index++) {
            deck.getOfficialMeta().incrementCardCount();
        }
        return deck;
    }
}
