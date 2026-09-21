package br.com.certamecards.card.service;

public record CardContent(String front, String back, String source) {

    public String normalizedSource() {
        return source == null || source.isBlank() ? null : source.strip();
    }
}
