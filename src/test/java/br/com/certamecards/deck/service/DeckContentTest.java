package br.com.certamecards.deck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DeckContentTest {

    @Test
    void givenNullDescription_whenNormalizing_thenReturnsNull() {
        assertThat(new DeckContent("Nome", null).normalizedDescription()).isNull();
    }

    @Test
    void givenBlankDescription_whenNormalizing_thenReturnsNull() {
        assertThat(new DeckContent("Nome", "   ").normalizedDescription()).isNull();
    }

    @Test
    void givenDescriptionWithSpaces_whenNormalizing_thenTrimsIt() {
        assertThat(new DeckContent("Nome", "  texto  ").normalizedDescription()).isEqualTo("texto");
    }

    @Test
    void givenBlankNameOrOversizedText_whenCreating_thenRejectsAsIllegalArgument() {
        assertThatThrownBy(() -> new DeckContent(" ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeckContent(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeckContent("x".repeat(121), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeckContent("Nome", "x".repeat(501))).isInstanceOf(IllegalArgumentException.class);
    }
}
