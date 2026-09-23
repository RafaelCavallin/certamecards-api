package br.com.certamecards.library.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.persistence.DeckSubscriptionStore;
import br.com.certamecards.library.persistence.SubscriberCountUpdater;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private final OfficialDeckAccess deckAccess;
    private final SubscriptionGuard guard;
    private final DeckSubscriptionStore store;
    private final SubscriberCountUpdater countUpdater;
    private final Clock clock;
    private final LibraryMetrics metrics;

    public SubscriptionService(
            OfficialDeckAccess deckAccess,
            SubscriptionGuard guard,
            DeckSubscriptionStore store,
            SubscriberCountUpdater countUpdater,
            Clock clock,
            LibraryMetrics metrics) {
        this.deckAccess = deckAccess;
        this.guard = guard;
        this.store = store;
        this.countUpdater = countUpdater;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public SubscriptionResult subscribe(UUID userId, UUID deckId) {
        try {
            return activate(userId, deckId);
        } catch (ApiException exception) {
            metrics.subscriptionRejected(exception.getErrorCode());
            throw exception;
        }
    }

    private SubscriptionResult activate(UUID userId, UUID deckId) {
        Deck deck = deckAccess.lockOfficial(deckId);
        Optional<DeckSubscription> previous = store.find(userId, deckId);
        guard.ensureCanSubscribe(userId, deck, previous);
        boolean restoredProgress = previous.isPresent() && store.hasStoredProgress(userId, deckId);
        DeckSubscription subscription = store.activate(userId, deckId, clock.instant());
        countUpdater.increment(deckId);
        metrics.subscription(previous.isPresent() ? "resubscribed" : "subscribed");
        return new SubscriptionResult(deck, subscription, restoredProgress);
    }

    @Transactional
    public void cancel(UUID userId, UUID deckId) {
        deckAccess.lockOfficial(deckId);
        if (store.find(userId, deckId).filter(DeckSubscription::active).isEmpty()) {
            throw ApiException.of(ErrorCode.NOT_SUBSCRIBED);
        }
        store.cancel(userId, deckId, clock.instant());
        countUpdater.decrement(deckId);
        metrics.subscription("cancelled");
    }
}
