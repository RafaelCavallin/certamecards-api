package br.com.certamecards.library.domain;

import java.time.Instant;
import java.util.UUID;

public record DeckSubscription(UUID deckId, Instant subscribedAt, Instant cancelledAt, long changeSeq) {

    public boolean active() {
        return cancelledAt == null;
    }
}
