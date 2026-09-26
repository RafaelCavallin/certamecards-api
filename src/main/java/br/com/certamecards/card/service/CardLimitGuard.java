package br.com.certamecards.card.service;

import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CardLimitGuard {

    private final CardRepository cardRepository;

    public CardLimitGuard(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public void ensureWithinLimits(UUID deckId, UUID ownerId) {
        if (cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId) >= CardLimits.MAX_CARDS_PER_DECK) {
            throw ApiException.of(ErrorCode.DECK_CARD_LIMIT);
        }
        if (cardRepository.countActiveByOwnerId(ownerId) >= CardLimits.MAX_CARDS_PER_USER) {
            throw ApiException.of(ErrorCode.USER_CARD_LIMIT);
        }
    }
}
