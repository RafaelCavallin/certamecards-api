package br.com.certamecards.library.domain;

import br.com.certamecards.deck.domain.Deck;

public record DuplicationSource(Deck deck, boolean subscribed) {}
