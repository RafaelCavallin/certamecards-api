package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CardLearningStateTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (CardLearningState state : CardLearningState.values()) {
            assertThat(CardLearningState.fromCode(state.code())).isEqualTo(state);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> CardLearningState.fromCode((short) 99)).isInstanceOf(IllegalArgumentException.class);
    }
}
