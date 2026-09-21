package br.com.certamecards.library.domain;

import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.common.error.CardLimitException;
import org.springframework.stereotype.Component;

@Component
public class UserCardLimitCalculator {

    public int availableCards(long usedCards) {
        return (int) Math.max(0, CardLimits.MAX_CARDS_PER_USER - usedCards);
    }

    public void ensureFits(long usedCards, int requiredCards) {
        int available = availableCards(usedCards);
        if (requiredCards > available) {
            throw new CardLimitException(requiredCards, available);
        }
    }
}
