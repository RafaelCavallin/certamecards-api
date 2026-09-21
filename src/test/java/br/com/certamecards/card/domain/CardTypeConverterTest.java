package br.com.certamecards.card.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardTypeConverterTest {

    private final CardTypeConverter converter = new CardTypeConverter();

    @Test
    void givenType_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(CardType.CLOZE)).isEqualTo("cloze");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsType() {
        assertThat(converter.convertToEntityAttribute("cloze")).isEqualTo(CardType.CLOZE);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
