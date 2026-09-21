package br.com.certamecards.deck.service;

import java.util.UUID;

public record UpdateDeckCommand(UUID subjectId, DeckContent content, int expectedVersion) {}
