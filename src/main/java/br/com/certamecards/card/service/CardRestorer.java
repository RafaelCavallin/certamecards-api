package br.com.certamecards.card.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.DeckService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CardRestorer {

    private final CardRepository cardRepository;
    private final DeckService deckService;
    private final CardLimitGuard limitGuard;
    private final Clock clock;

    public CardRestorer(
            CardRepository cardRepository, DeckService deckService, CardLimitGuard limitGuard, Clock clock) {
        this.cardRepository = cardRepository;
        this.deckService = deckService;
        this.limitGuard = limitGuard;
        this.clock = clock;
    }

    @Transactional
    public Card restore(UUID ownerId, Card card, UUID targetDeckId, CardContent content) {
        Deck targetDeck = deckService.lockOwned(ownerId, targetDeckId);
        if (targetDeck.isDeleted()) {
            throw ApiException.of(ErrorCode.NOT_FOUND);
        }
        limitGuard.ensureWithinLimits(targetDeck.getId(), ownerId);
        card.moveToDeck(targetDeck.getId());
        card.editContent(content.front().strip(), content.back().strip(), content.normalizedSource());
        card.getAudit().restore(clock.instant());
        return cardRepository.saveAndFlush(card);
    }
}
