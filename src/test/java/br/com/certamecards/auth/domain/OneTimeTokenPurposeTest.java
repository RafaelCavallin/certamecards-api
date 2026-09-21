package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OneTimeTokenPurposeTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (OneTimeTokenPurpose purpose : OneTimeTokenPurpose.values()) {
            assertThat(OneTimeTokenPurpose.fromCode(purpose.code())).isEqualTo(purpose);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> OneTimeTokenPurpose.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
