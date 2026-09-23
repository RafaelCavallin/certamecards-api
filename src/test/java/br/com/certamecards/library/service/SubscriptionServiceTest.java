package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.persistence.DeckSubscriptionStore;
import br.com.certamecards.library.persistence.SubscriberCountUpdater;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T14:00:00Z");

    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final SubscriptionGuard guard = mock(SubscriptionGuard.class);
    private final DeckSubscriptionStore store = mock(DeckSubscriptionStore.class);
    private final SubscriberCountUpdater countUpdater = mock(SubscriberCountUpdater.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SubscriptionService service = new SubscriptionService(
            deckAccess, guard, store, countUpdater, Clock.fixed(NOW, ZoneOffset.UTC), new LibraryMetrics(registry));
    private final UUID userId = UUID.randomUUID();
    private final Deck deck =
            new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
    private final DeckSubscription activated = new DeckSubscription(deck.getId(), NOW, null, 10);

    @Test
    void givenFirstSubscription_whenSubscribing_thenActivatesCountsAndDoesNotRestoreProgress() {
        when(deckAccess.lockOfficial(deck.getId())).thenReturn(deck);
        when(store.find(userId, deck.getId())).thenReturn(Optional.empty());
        when(store.activate(userId, deck.getId(), NOW)).thenReturn(activated);

        SubscriptionResult result = service.subscribe(userId, deck.getId());

        assertThat(result.subscription()).isEqualTo(activated);
        assertThat(result.restoredProgress()).isFalse();
        verify(countUpdater).increment(deck.getId());
        verify(store, never()).hasStoredProgress(userId, deck.getId());
    }

    @Test
    void givenCancelledSubscriptionWithProgress_whenSubscribing_thenRestoresProgress() {
        DeckSubscription cancelled = new DeckSubscription(deck.getId(), NOW.minusSeconds(60), NOW.minusSeconds(30), 5);
        when(deckAccess.lockOfficial(deck.getId())).thenReturn(deck);
        when(store.find(userId, deck.getId())).thenReturn(Optional.of(cancelled));
        when(store.hasStoredProgress(userId, deck.getId())).thenReturn(true);
        when(store.activate(userId, deck.getId(), NOW)).thenReturn(activated);

        assertThat(service.subscribe(userId, deck.getId()).restoredProgress()).isTrue();
    }

    @Test
    void givenCancelledSubscriptionWithoutProgress_whenSubscribing_thenDoesNotRestoreProgress() {
        DeckSubscription cancelled = new DeckSubscription(deck.getId(), NOW.minusSeconds(60), NOW.minusSeconds(30), 5);
        when(deckAccess.lockOfficial(deck.getId())).thenReturn(deck);
        when(store.find(userId, deck.getId())).thenReturn(Optional.of(cancelled));
        when(store.hasStoredProgress(userId, deck.getId())).thenReturn(false);
        when(store.activate(userId, deck.getId(), NOW)).thenReturn(activated);

        assertThat(service.subscribe(userId, deck.getId()).restoredProgress()).isFalse();
    }

    @Test
    void givenActiveSubscription_whenCancelling_thenMarksCancelledAndDecrementsCount() {
        when(store.find(userId, deck.getId())).thenReturn(Optional.of(activated));

        service.cancel(userId, deck.getId());

        verify(deckAccess).lockOfficial(deck.getId());
        verify(store).cancel(userId, deck.getId(), NOW);
        verify(countUpdater).decrement(deck.getId());
    }

    @Test
    void givenNoActiveSubscription_whenCancelling_thenThrowsNotSubscribed() {
        when(store.find(userId, deck.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(userId, deck.getId()))
                .isInstanceOfSatisfying(
                        ApiException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_SUBSCRIBED));
        verify(countUpdater, never()).decrement(deck.getId());
    }

    @Test
    void givenSubscriptionOutcomes_whenSubscribingAndCancelling_thenCountsEachResult() {
        DeckSubscription cancelled = new DeckSubscription(deck.getId(), NOW.minusSeconds(60), NOW.minusSeconds(30), 5);
        when(deckAccess.lockOfficial(deck.getId())).thenReturn(deck);
        when(store.find(userId, deck.getId()))
                .thenReturn(Optional.empty(), Optional.of(cancelled), Optional.of(activated));
        when(store.activate(userId, deck.getId(), NOW)).thenReturn(activated);

        service.subscribe(userId, deck.getId());
        service.subscribe(userId, deck.getId());
        service.cancel(userId, deck.getId());

        assertThat(registry.counter("library.subscription", "result", "subscribed")
                        .count())
                .isEqualTo(1);
        assertThat(registry.counter("library.subscription", "result", "resubscribed")
                        .count())
                .isEqualTo(1);
        assertThat(registry.counter("library.subscription", "result", "cancelled")
                        .count())
                .isEqualTo(1);
    }

    @Test
    void givenRejectedSubscriptions_whenSubscribing_thenCountsLimitAndUnavailable() {
        when(deckAccess.lockOfficial(deck.getId())).thenReturn(deck);
        when(store.find(userId, deck.getId())).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(
                        ApiException.of(ErrorCode.USER_CARD_LIMIT),
                        ApiException.of(ErrorCode.DECK_NOT_AVAILABLE),
                        ApiException.of(ErrorCode.ALREADY_SUBSCRIBED))
                .when(guard)
                .ensureCanSubscribe(userId, deck, Optional.empty());

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> service.subscribe(userId, deck.getId())).isInstanceOf(ApiException.class);
        }

        assertThat(registry.counter("library.subscription", "result", "limit_rejected")
                        .count())
                .isEqualTo(1);
        assertThat(registry.counter("library.subscription", "result", "unavailable")
                        .count())
                .isEqualTo(1);
        assertThat(registry.find("library.subscription").counters()).hasSize(2);
    }
}
