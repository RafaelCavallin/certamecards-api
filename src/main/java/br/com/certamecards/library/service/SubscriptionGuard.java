package br.com.certamecards.library.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.domain.UserCardLimitCalculator;
import br.com.certamecards.library.persistence.UserCardCountsQuery;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionGuard {

    private final DeckAvailabilityGuard availabilityGuard;
    private final UserCardCountsQuery cardCountsQuery;
    private final UserCardLimitCalculator limitCalculator;

    public SubscriptionGuard(
            DeckAvailabilityGuard availabilityGuard,
            UserCardCountsQuery cardCountsQuery,
            UserCardLimitCalculator limitCalculator) {
        this.availabilityGuard = availabilityGuard;
        this.cardCountsQuery = cardCountsQuery;
        this.limitCalculator = limitCalculator;
    }

    public void ensureCanSubscribe(UUID userId, Deck deck, Optional<DeckSubscription> previous) {
        availabilityGuard.ensurePublished(deck);
        if (previous.filter(DeckSubscription::active).isPresent()) {
            throw ApiException.of(ErrorCode.ALREADY_SUBSCRIBED);
        }
        limitCalculator.ensureFits(
                cardCountsQuery.usedCards(userId), deck.getOfficialMeta().getCardCount());
    }
}
