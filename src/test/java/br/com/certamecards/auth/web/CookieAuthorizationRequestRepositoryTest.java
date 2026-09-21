package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class CookieAuthorizationRequestRepositoryTest {

    private static final String KEY = "QabpsXTm+MPu629aETSuCTTfJn+qhnPjeQp3KPGn8CQ=";

    private final CookieCipher cipher = new CookieCipher(new CookieEncryptionProperties(KEY));
    private final CookieAuthorizationRequestRepository repository = new CookieAuthorizationRequestRepository(cipher);

    @Test
    void givenNoCookie_whenLoading_thenReturnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(repository.loadAuthorizationRequest(request)).isNull();
    }

    @Test
    void givenAuthorizationRequest_whenSavedAndReloaded_thenValuesMatch() {
        OAuth2AuthorizationRequest authorizationRequest = buildAuthorizationRequest();
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        saveRequest.setParameter("intent", "login");
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest, saveRequest, saveResponse);

        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(saveResponse.getCookies());
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

        assertThat(loaded.getClientId()).isEqualTo(authorizationRequest.getClientId());
        assertThat(repository.readIntent(loadRequest)).isEqualTo("login");
    }

    @Test
    void givenNoIntentParameter_whenSaving_thenDefaultsToLoginIntent() {
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(buildAuthorizationRequest(), saveRequest, saveResponse);

        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(saveResponse.getCookies());
        assertThat(repository.readIntent(loadRequest)).isEqualTo("login");
    }

    @Test
    void givenNoCookie_whenReadingIntent_thenDefaultsToLogin() {
        assertThat(repository.readIntent(new MockHttpServletRequest())).isEqualTo("login");
    }

    @Test
    void givenNullAuthorizationRequest_whenSaving_thenRemovesCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(null, request, response);

        assertThat(response.getCookie("certame_oauth2_req").getValue()).isEmpty();
    }

    @Test
    void givenSavedRequest_whenRemoving_thenReturnsItAndExpiresCookie() {
        OAuth2AuthorizationRequest authorizationRequest = buildAuthorizationRequest();
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest, saveRequest, saveResponse);
        MockHttpServletRequest removeRequest = new MockHttpServletRequest();
        removeRequest.setCookies(saveResponse.getCookies());
        MockHttpServletResponse removeResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(removeRequest, removeResponse);

        assertThat(removed.getClientId()).isEqualTo(authorizationRequest.getClientId());
    }

    private OAuth2AuthorizationRequest buildAuthorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-id")
                .redirectUri("https://certamecards.localhost/api/auth/oauth2/callback/google")
                .scopes(Map.of("openid", "openid").keySet())
                .state("state-value")
                .build();
    }
}
