package br.com.certamecards.settings.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ThemeTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (Theme theme : Theme.values()) {
            assertThat(Theme.fromCode(theme.code())).isEqualTo(theme);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> Theme.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
