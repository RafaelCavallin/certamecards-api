package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ReviewPushContext(UUID userId, Map<UUID, Card> accessibleCards, Instant now) {}
