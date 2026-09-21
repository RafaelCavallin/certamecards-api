package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReviewKindConverterTest {

    private final ReviewKindConverter converter = new ReviewKindConverter();

    @Test
    void givenKind_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(ReviewKind.RESET)).isEqualTo("reset");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsKind() {
        assertThat(converter.convertToEntityAttribute("reset")).isEqualTo(ReviewKind.RESET);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
