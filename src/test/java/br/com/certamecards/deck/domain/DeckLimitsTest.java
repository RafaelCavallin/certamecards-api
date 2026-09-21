package br.com.certamecards.deck.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeckLimitsTest {

    @Test
    void givenDeckLimits_whenReadingConstants_thenValuesMatchTechSpec() {
        assertThat(DeckLimits.MAX_CARDS_PER_DECK).isEqualTo(5_000);
        assertThat(DeckLimits.MAX_CARDS_PER_USER).isEqualTo(50_000);
        assertThat(DeckLimits.MAX_NAME_LENGTH).isEqualTo(120);
        assertThat(DeckLimits.MAX_DESCRIPTION_LENGTH).isEqualTo(500);
    }
}
