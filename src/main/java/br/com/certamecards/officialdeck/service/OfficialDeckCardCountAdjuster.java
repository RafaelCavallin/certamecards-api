package br.com.certamecards.officialdeck.service;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OfficialDeckCardCountAdjuster {

    private final OfficialDeckAccess deckAccess;

    public OfficialDeckCardCountAdjuster(OfficialDeckAccess deckAccess) {
        this.deckAccess = deckAccess;
    }

    public void increment(Deck deck, Instant now) {
        deck.getOfficialMeta().incrementCardCount();
        touchAndSave(deck, now);
    }

    public void decrement(Deck deck, Instant now) {
        deck.getOfficialMeta().decrementCardCount();
        touchAndSave(deck, now);
    }

    public void touchAndSave(Deck deck, Instant now) {
        deck.getOfficialMeta().touchContentUpdatedAt(now);
        deck.touch(now);
        deckAccess.save(deck);
    }
}
