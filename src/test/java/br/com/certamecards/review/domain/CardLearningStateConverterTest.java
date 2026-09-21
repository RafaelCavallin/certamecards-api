package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardLearningStateConverterTest {

    private final CardLearningStateConverter converter = new CardLearningStateConverter();

    @Test
    void givenState_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(CardLearningState.REVIEW)).isEqualTo((short) 2);
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsState() {
        assertThat(converter.convertToEntityAttribute((short) 2)).isEqualTo(CardLearningState.REVIEW);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
