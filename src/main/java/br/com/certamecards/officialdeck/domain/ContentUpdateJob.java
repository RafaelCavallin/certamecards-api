package br.com.certamecards.officialdeck.domain;

import java.time.Instant;
import java.util.UUID;

public record ContentUpdateJob(UUID id, UUID cardId, UUID deckId, String note, Instant updatedAt, UUID cursorUserId) {}
