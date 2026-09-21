package br.com.certamecards.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRoleConverterTest {

    private final UserRoleConverter converter = new UserRoleConverter();

    @Test
    void givenRole_whenConvertingToDatabaseColumn_thenReturnsCode() {
        assertThat(converter.convertToDatabaseColumn(UserRole.ADMIN)).isEqualTo("admin");
    }

    @Test
    void givenNull_whenConvertingToDatabaseColumn_thenReturnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void givenCode_whenConvertingToEntityAttribute_thenReturnsRole() {
        assertThat(converter.convertToEntityAttribute("candidate")).isEqualTo(UserRole.CANDIDATE);
    }

    @Test
    void givenNull_whenConvertingToEntityAttribute_thenReturnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
