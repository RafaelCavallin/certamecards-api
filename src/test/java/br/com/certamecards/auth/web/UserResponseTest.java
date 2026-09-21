package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserResponseTest {

    @Test
    void givenUserWithoutAcceptedTerms_whenBuilding_thenTermsAcceptedIsFalse() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);

        UserResponse response = UserResponse.from(user);

        assertThat(response.termsAccepted()).isFalse();
    }

    @Test
    void givenUserWithAcceptedTerms_whenBuilding_thenTermsAcceptedIsTrue() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        user.acceptTerms("2026-09-01", Instant.parse("2026-09-17T12:00:00Z"));

        UserResponse response = UserResponse.from(user);

        assertThat(response.termsAccepted()).isTrue();
    }
}
