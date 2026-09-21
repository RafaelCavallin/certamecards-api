package br.com.certamecards.library.service;

import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.library.domain.DuplicateDeckCommand;
import br.com.certamecards.library.domain.DuplicationPlan;
import br.com.certamecards.library.domain.DuplicationSource;
import br.com.certamecards.library.domain.UserCardLimitCalculator;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import org.springframework.stereotype.Component;

@Component
public class DuplicationPlanner {

    private final UserCardLimitCalculator limitCalculator;

    public DuplicationPlanner(UserCardLimitCalculator limitCalculator) {
        this.limitCalculator = limitCalculator;
    }

    public DuplicationPlan plan(DuplicateDeckCommand command, DuplicationSource source, long usedCards) {
        ensureDuplicable(source);
        int cardCount = source.deck().getOfficialMeta().getCardCount();
        boolean cancel = source.subscribed() && command.options().cancelSubscription();
        long usedAfterCancel = cancel ? usedCards - cardCount : usedCards;
        if (cardCount > CardLimits.MAX_CARDS_PER_DECK) {
            throw ApiException.of(ErrorCode.DECK_CARD_LIMIT);
        }
        limitCalculator.ensureFits(usedAfterCancel, cardCount);
        return new DuplicationPlan(source.subscribed() && command.options().carryProgress(), cancel);
    }

    private void ensureDuplicable(DuplicationSource source) {
        Deck deck = source.deck();
        if (deck.isDeleted()) {
            throw ApiException.of(ErrorCode.NOT_FOUND);
        }
        String status = deck.getOfficialMeta().getOfficialStatus();
        if (OfficialDeckStatus.DRAFT.code().equals(status)) {
            throw ApiException.of(ErrorCode.DECK_NOT_AVAILABLE);
        }
        if (OfficialDeckStatus.DISCONTINUED.code().equals(status) && !source.subscribed()) {
            throw ApiException.of(ErrorCode.NOT_SUBSCRIBED);
        }
    }
}
