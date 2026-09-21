package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record DeckSubscriptionChange(UUID deckId, Instant subscribedAt, Instant cancelledAt, long changeSeq) {}
