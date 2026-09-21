package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record StateReconciliationRequest(
        UUID userId, List<CardStatePushInput> states, Map<UUID, Card> accessibleCards, Instant now) {}
