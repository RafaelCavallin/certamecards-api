package br.com.certamecards.card.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardLimitsTest {

    @Test
    void givenCardLimits_whenReadingConstants_thenValuesMatchTechSpec() {
        assertThat(CardLimits.MAX_FRONT_LENGTH).isEqualTo(1_000);
        assertThat(CardLimits.MAX_BACK_LENGTH).isEqualTo(2_000);
        assertThat(CardLimits.MAX_SOURCE_LENGTH).isEqualTo(120);
    }
}
