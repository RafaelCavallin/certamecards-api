package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.service.AccessTokenIssuer;
import br.com.certamecards.auth.service.GoogleLinkService;
import br.com.certamecards.auth.service.GoogleLoginHandler;
import br.com.certamecards.auth.service.GoogleLoginOutcome;
import br.com.certamecards.auth.service.IssuedRefreshToken;
import br.com.certamecards.auth.service.RefreshCookieProperties;
import br.com.certamecards.auth.service.RefreshTokenService;
import br.com.certamecards.common.security.ClientOriginProperties;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleAuthenticationSuccessHandlerTest {

    private static final String ORIGIN = "https://certamecards.localhost";
    private static final String SUBJECT = "google-subject-1";

    private final GoogleLoginHandler loginHandler = mock(GoogleLoginHandler.class);
    private final GoogleLinkService linkService = mock(GoogleLinkService.class);
    private final AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final RefreshCookieFactory cookieFactory =
            new RefreshCookieFactory(new RefreshCookieProperties("__Host-certame_rt", Duration.ofDays(30), true));
    private final CookieAuthorizationRequestRepository authorizationRequestRepository =
            new CookieAuthorizationRequestRepository(
                    new CookieCipher(new CookieEncryptionProperties("QabpsXTm+MPu629aETSuCTTfJn+qhnPjeQp3KPGn8CQ=")));
    private final ClientOriginProperties originProperties = new ClientOriginProperties(ORIGIN, "web");
    private final GoogleAuthenticationSuccessHandler handler = new GoogleAuthenticationSuccessHandler(
            loginHandler,
            linkService,
            accessTokenIssuer,
            refreshTokenService,
            cookieFactory,
            authorizationRequestRepository,
            originProperties);

    @Test
    void givenLoginIntent_whenConcluded_thenIssuesRefreshCookieAndRedirectsToFinishLogin() {
        UUID userId = UUID.randomUUID();
        when(loginHandler.handleLogin(any())).thenReturn(new GoogleLoginOutcome.Concluded(userId));
        when(refreshTokenService.issueNewFamily(userId, null)).thenReturn(new IssuedRefreshToken("raw-refresh", null));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, fakeAuthentication());

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).isEqualTo(ORIGIN + "/entrar/concluir");
        assertThat(response.getHeader("Set-Cookie")).contains("__Host-certame_rt");
    }

    @Test
    void givenExistingPasswordAccount_whenLinkRequired_thenRedirectsToLinkPage() {
        when(loginHandler.handleLogin(any())).thenReturn(new GoogleLoginOutcome.LinkRequired("raw-link-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, fakeAuthentication());

        assertThat(response.getHeader("Location")).isEqualTo(ORIGIN + "/entrar/vincular#token=raw-link-token");
    }

    @Test
    void givenReauthIntentCookie_whenSucceeding_thenRedirectsToDeleteAccountConfirmation() {
        when(linkService.handleReauth(SUBJECT)).thenReturn("raw-reauth-token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("certame_oauth2_intent", "reauth"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, fakeAuthentication());

        assertThat(response.getHeader("Location")).isEqualTo(ORIGIN + "/ajustes/excluir-conta#reauth=raw-reauth-token");
    }

    private Authentication fakeAuthentication() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getSubject()).thenReturn(SUBJECT);
        when(oidcUser.getEmail()).thenReturn("ana@exemplo.com");
        when(oidcUser.getEmailVerified()).thenReturn(Boolean.TRUE);
        when(oidcUser.getFullName()).thenReturn("Ana");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        return authentication;
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
