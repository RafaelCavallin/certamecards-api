package br.com.certamecards.deck.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DeckOriginTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (DeckOrigin origin : DeckOrigin.values()) {
            assertThat(DeckOrigin.fromCode(origin.code())).isEqualTo(origin);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> DeckOrigin.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
