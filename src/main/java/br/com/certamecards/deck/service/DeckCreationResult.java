package br.com.certamecards.deck.service;

import br.com.certamecards.deck.domain.Deck;

public record DeckCreationResult(Deck deck, boolean created) {}
