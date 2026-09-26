package br.com.certamecards.card.service;

import br.com.certamecards.card.domain.CardLimits;

public record CardContent(String front, String back, String source) {

    public CardContent {
        requireText(front, CardLimits.MAX_FRONT_LENGTH);
        requireText(back, CardLimits.MAX_BACK_LENGTH);
        if (source != null && source.length() > CardLimits.MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException("source too long");
        }
    }

    public String normalizedSource() {
        return source == null || source.isBlank() ? null : source.strip();
    }

    private static void requireText(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException("invalid card text");
        }
    }
}
