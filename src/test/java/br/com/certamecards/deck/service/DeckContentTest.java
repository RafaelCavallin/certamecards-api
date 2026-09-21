package br.com.certamecards.deck.service;

import static org.assertj.core.api.Assertions.assertThat;

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
}
