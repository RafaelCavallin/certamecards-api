package br.com.certamecards.officialdeck.domain;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.officialdeck.persistence.DeckSubscriptionsQuery;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OfficialDeckDeletionGuard {

    private final DeckSubscriptionsQuery subscriptionsQuery;

    public OfficialDeckDeletionGuard(DeckSubscriptionsQuery subscriptionsQuery) {
        this.subscriptionsQuery = subscriptionsQuery;
    }

    public void ensureDeletable(UUID deckId) {
        if (subscriptionsQuery.everHadSubscribers(deckId)) {
            throw ApiException.of(ErrorCode.OFFICIAL_DECK_HAS_SUBSCRIBERS);
        }
    }
}
