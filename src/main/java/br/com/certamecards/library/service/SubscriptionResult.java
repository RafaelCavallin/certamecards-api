package br.com.certamecards.library.service;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.library.domain.DeckSubscription;

public record SubscriptionResult(Deck deck, DeckSubscription subscription, boolean restoredProgress) {}
