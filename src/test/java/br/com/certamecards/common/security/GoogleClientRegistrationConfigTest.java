package br.com.certamecards.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class GoogleClientRegistrationConfigTest {

    private final GoogleClientRegistrationConfig config = new GoogleClientRegistrationConfig();

    @Test
    void givenNoCredentials_whenBuildingRepository_thenGoogleRegistrationIsAbsent() {
        ClientRegistrationRepository repository =
                config.clientRegistrationRepository(new GoogleOAuthProperties(null, null));

        assertThat(repository.findByRegistrationId("google")).isNull();
    }

    @Test
    void givenCredentials_whenBuildingRepository_thenGoogleRegistrationIsPresent() {
        ClientRegistrationRepository repository =
                config.clientRegistrationRepository(new GoogleOAuthProperties("client-id", "client-secret"));

        ClientRegistration registration = repository.findByRegistrationId("google");

        assertThat(registration).isNotNull();
        assertThat(registration.getClientId()).isEqualTo("client-id");
    }
}
