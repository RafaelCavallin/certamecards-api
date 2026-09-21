package br.com.certamecards.card.service;

import java.util.UUID;

public record CreateCardCommand(UUID id, UUID deckId, UUID ownerId, CardContent content) {}
