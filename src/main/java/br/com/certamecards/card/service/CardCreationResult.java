package br.com.certamecards.card.service;

import br.com.certamecards.card.domain.Card;

public record CardCreationResult(Card card, boolean created) {}
