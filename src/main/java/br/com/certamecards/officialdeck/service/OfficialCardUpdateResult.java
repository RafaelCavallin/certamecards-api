package br.com.certamecards.officialdeck.service;

import br.com.certamecards.card.domain.Card;

public record OfficialCardUpdateResult(Card card, int affectedSubscribers, boolean contentUpdateQueued) {}
