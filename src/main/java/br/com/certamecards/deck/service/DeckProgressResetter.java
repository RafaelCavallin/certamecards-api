package br.com.certamecards.deck.service;

import br.com.certamecards.common.sync.SyncSequenceQuery;
import br.com.certamecards.deck.persistence.DeckCardsQuery;
import br.com.certamecards.review.service.CardStateService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeckProgressResetter {

    private final DeckService deckService;
    private final DeckCardsQuery deckCardsQuery;
    private final CardStateService cardStateService;
    private final SyncSequenceQuery syncSequenceQuery;
    private final Clock clock;

    public DeckProgressResetter(
            DeckService deckService,
            DeckCardsQuery deckCardsQuery,
            CardStateService cardStateService,
            SyncSequenceQuery syncSequenceQuery,
            Clock clock) {
        this.deckService = deckService;
        this.deckCardsQuery = deckCardsQuery;
        this.cardStateService = cardStateService;
        this.syncSequenceQuery = syncSequenceQuery;
        this.clock = clock;
    }

    @Transactional
    public ResetProgressResult reset(UUID userId, UUID deckId) {
        deckService.findAccessible(userId, deckId);
        List<UUID> cardIds = deckCardsQuery.activeCardIds(deckId);
        int resetCards = cardStateService.resetCards(userId, cardIds, clock.instant());
        return new ResetProgressResult(resetCards, syncSequenceQuery.currentValue());
    }
}
