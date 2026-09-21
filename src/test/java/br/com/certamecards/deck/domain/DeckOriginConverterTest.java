package br.com.certamecards.deck.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeckOriginConverterTest {

    private final DeckOriginConverter converter = new DeckOriginConverter();

    @Test
    void givenOrigin_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(DeckOrigin.OWN)).isEqualTo("own");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsOrigin() {
        assertThat(converter.convertToEntityAttribute("own")).isEqualTo(DeckOrigin.OWN);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
