package br.com.certamecards.officialdeck.domain;

import java.util.UUID;

public record PurgeTarget(UUID userId, UUID deckId) {}
