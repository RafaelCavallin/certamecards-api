package br.com.certamecards.deck.service;

public record DeckContent(String name, String description) {

    public String normalizedDescription() {
        return description == null || description.isBlank() ? null : description.strip();
    }
}
