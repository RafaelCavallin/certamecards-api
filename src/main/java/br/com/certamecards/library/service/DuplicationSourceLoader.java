package br.com.certamecards.library.service;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.domain.DuplicationSource;
import br.com.certamecards.library.persistence.DeckSubscriptionStore;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DuplicationSourceLoader {

    private final OfficialDeckAccess deckAccess;
    private final DeckSubscriptionStore subscriptionStore;

    public DuplicationSourceLoader(OfficialDeckAccess deckAccess, DeckSubscriptionStore subscriptionStore) {
        this.deckAccess = deckAccess;
        this.subscriptionStore = subscriptionStore;
    }

    public DuplicationSource load(UUID userId, UUID deckId) {
        Deck deck = deckAccess.lockOfficial(deckId);
        boolean subscribed = subscriptionStore
                .find(userId, deckId)
                .filter(DeckSubscription::active)
                .isPresent();
        return new DuplicationSource(deck, subscribed);
    }
}
