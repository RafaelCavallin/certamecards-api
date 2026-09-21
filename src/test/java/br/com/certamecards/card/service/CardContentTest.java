package br.com.certamecards.card.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardContentTest {

    @Test
    void givenNullSource_whenNormalizing_thenReturnsNull() {
        assertThat(new CardContent("Frente", "Verso", null).normalizedSource()).isNull();
    }

    @Test
    void givenBlankSource_whenNormalizing_thenReturnsNull() {
        assertThat(new CardContent("Frente", "Verso", "   ").normalizedSource()).isNull();
    }

    @Test
    void givenSourceWithSpaces_whenNormalizing_thenTrimsIt() {
        assertThat(new CardContent("Frente", "Verso", "  CF/88  ").normalizedSource())
                .isEqualTo("CF/88");
    }
}
