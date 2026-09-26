package br.com.certamecards.deck.service;

import br.com.certamecards.deck.domain.DeckLimits;

public record DeckContent(String name, String description) {

    public DeckContent {
        if (name == null || name.isBlank() || name.length() > DeckLimits.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("invalid deck name");
        }
        if (description != null && description.length() > DeckLimits.MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("description too long");
        }
    }

    public String normalizedDescription() {
        return description == null || description.isBlank() ? null : description.strip();
    }
}
