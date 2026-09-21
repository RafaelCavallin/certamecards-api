package br.com.certamecards.events.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductEventNameConverterTest {

    private final ProductEventNameConverter converter = new ProductEventNameConverter();

    @Test
    void givenName_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(ProductEventName.DECK_CREATED))
                .isEqualTo("deck_created");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsName() {
        assertThat(converter.convertToEntityAttribute("deck_created")).isEqualTo(ProductEventName.DECK_CREATED);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
