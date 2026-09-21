package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.common.security.ClientOriginProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class GoogleAuthenticationFailureHandlerTest {

    private final ClientOriginProperties originProperties =
            new ClientOriginProperties("https://certamecards.localhost", "web");
    private final GoogleAuthenticationFailureHandler handler = new GoogleAuthenticationFailureHandler(originProperties);

    @Test
    void givenAuthenticationFailure_whenHandling_thenRedirectsToLoginErrorPage() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, new BadCredentialsException("failed"));

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo("https://certamecards.localhost/entrar?erro=google");
    }
}
