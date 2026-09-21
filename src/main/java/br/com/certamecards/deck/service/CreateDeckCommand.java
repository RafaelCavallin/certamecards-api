package br.com.certamecards.deck.service;

import java.util.UUID;

public record CreateDeckCommand(UUID id, UUID ownerId, UUID subjectId, DeckContent content) {}
