package br.com.certamecards.library.service;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.library.domain.DuplicationCounts;

public record DuplicationResult(Deck deck, DuplicationCounts counts, boolean created) {}
