package br.com.certamecards.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void givenBlankOrOversizedText_whenCreating_thenRejectsAsIllegalArgument() {
        assertThatThrownBy(() -> new CardContent("  ", "Verso", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CardContent("Frente", null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CardContent("x".repeat(1_001), "Verso", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CardContent("Frente", "x".repeat(2_001), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CardContent("Frente", "Verso", "x".repeat(121)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
