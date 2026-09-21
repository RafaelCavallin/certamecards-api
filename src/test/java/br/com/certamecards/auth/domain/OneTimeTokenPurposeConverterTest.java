package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OneTimeTokenPurposeConverterTest {

    private final OneTimeTokenPurposeConverter converter = new OneTimeTokenPurposeConverter();

    @Test
    void givenPurpose_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(OneTimeTokenPurpose.REAUTH))
                .isEqualTo("reauth");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsPurpose() {
        assertThat(converter.convertToEntityAttribute("reauth")).isEqualTo(OneTimeTokenPurpose.REAUTH);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
