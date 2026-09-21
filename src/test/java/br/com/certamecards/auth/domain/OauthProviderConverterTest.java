package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OauthProviderConverterTest {

    private final OauthProviderConverter converter = new OauthProviderConverter();

    @Test
    void givenProvider_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(OauthProvider.GOOGLE)).isEqualTo("google");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsProvider() {
        assertThat(converter.convertToEntityAttribute("google")).isEqualTo(OauthProvider.GOOGLE);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
