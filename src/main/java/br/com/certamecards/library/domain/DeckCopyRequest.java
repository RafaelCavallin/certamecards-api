package br.com.certamecards.library.domain;

import br.com.certamecards.deck.domain.Deck;
import java.util.UUID;

public record DeckCopyRequest(UUID userId, Deck source, UUID newDeckId, boolean carryStates) {}
