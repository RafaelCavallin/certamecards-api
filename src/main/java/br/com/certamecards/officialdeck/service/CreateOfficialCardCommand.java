package br.com.certamecards.officialdeck.service;

import java.util.UUID;

public record CreateOfficialCardCommand(UUID id, UUID deckId, String front, String back, String source) {}
