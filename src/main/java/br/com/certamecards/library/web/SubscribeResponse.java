package br.com.certamecards.library.web;

import br.com.certamecards.deck.web.DeckResponse;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.service.SubscriptionResult;

public record SubscribeResponse(DeckResponse deck, DeckSubscription subscription, boolean restoredProgress) {

    public static SubscribeResponse from(SubscriptionResult result) {
        return new SubscribeResponse(
                DeckResponse.from(result.deck()), result.subscription(), result.restoredProgress());
    }
}
