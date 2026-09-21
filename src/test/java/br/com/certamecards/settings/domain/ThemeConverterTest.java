package br.com.certamecards.settings.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThemeConverterTest {

    private final ThemeConverter converter = new ThemeConverter();

    @Test
    void givenTheme_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(Theme.DIA)).isEqualTo("dia");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsTheme() {
        assertThat(converter.convertToEntityAttribute("dia")).isEqualTo(Theme.DIA);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
