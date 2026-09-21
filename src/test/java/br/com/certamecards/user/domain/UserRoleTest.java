package br.com.certamecards.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void givenEachCode_whenFromCode_thenReturnsMatchingConstant() {
        for (UserRole role : UserRole.values()) {
            assertThat(UserRole.fromCode(role.code())).isEqualTo(role);
        }
    }

    @Test
    void givenUnknownCode_whenFromCode_thenThrows() {
        assertThatThrownBy(() -> UserRole.fromCode("unknown")).isInstanceOf(IllegalArgumentException.class);
    }
}
