package br.com.certamecards.library.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.review.domain.CardState;
import java.util.List;
import java.util.UUID;

public record DeckContentPage(List<Card> cards, List<CardState> cardStates, UUID nextAfter, boolean hasMore) {}
