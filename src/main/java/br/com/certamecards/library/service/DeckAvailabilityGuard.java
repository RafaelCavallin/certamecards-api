package br.com.certamecards.library.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import org.springframework.stereotype.Component;

@Component
public class DeckAvailabilityGuard {

    public void ensurePublished(Deck deck) {
        if (deck.isDeleted()) {
            throw ApiException.of(ErrorCode.NOT_FOUND);
        }
        if (!OfficialDeckStatus.PUBLISHED.code().equals(deck.getOfficialMeta().getOfficialStatus())) {
            throw ApiException.of(ErrorCode.DECK_NOT_AVAILABLE);
        }
    }
}
