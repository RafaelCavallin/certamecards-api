package br.com.certamecards.card.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CardTypeTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (CardType type : CardType.values()) {
            assertThat(CardType.fromCode(type.code())).isEqualTo(type);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> CardType.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
