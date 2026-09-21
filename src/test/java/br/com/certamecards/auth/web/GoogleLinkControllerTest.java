package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.service.AccessTokenIssuer;
import br.com.certamecards.auth.service.GoogleLinkService;
import br.com.certamecards.auth.service.IssuedAccessToken;
import br.com.certamecards.auth.service.IssuedRefreshToken;
import br.com.certamecards.auth.service.RefreshCookieProperties;
import br.com.certamecards.auth.service.RefreshTokenService;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GoogleLinkControllerTest {

    private final GoogleLinkService googleLinkService = mock(GoogleLinkService.class);
    private final AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final RefreshCookieFactory cookieFactory =
            new RefreshCookieFactory(new RefreshCookieProperties("__Host-certame_rt", Duration.ofDays(30), true));
    private final GoogleLinkController controller =
            new GoogleLinkController(googleLinkService, accessTokenIssuer, refreshTokenService, cookieFactory);

    @Test
    void givenValidLinkRequest_whenLinking_thenReturnsAuthResponseWithCookie() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        when(googleLinkService.linkWithPassword("raw-token", "senha")).thenReturn(user);
        when(accessTokenIssuer.issue(user.getId())).thenReturn(new IssuedAccessToken("access-token", 900));
        when(refreshTokenService.issueNewFamily(user.getId(), "test-agent"))
                .thenReturn(new IssuedRefreshToken("raw-refresh", null));
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("User-Agent", "test-agent");

        ResponseEntity<AuthResponse> response =
                controller.link(new GoogleLinkRequest("raw-token", "senha"), httpRequest);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("__Host-certame_rt");
    }
}
