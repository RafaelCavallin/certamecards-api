package br.com.certamecards.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OauthProviderTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (OauthProvider provider : OauthProvider.values()) {
            assertThat(OauthProvider.fromCode(provider.code())).isEqualTo(provider);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> OauthProvider.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
