package br.com.certamecards.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.service.AccessTokenIssuer;
import br.com.certamecards.auth.service.IssuedAccessToken;
import br.com.certamecards.auth.service.IssuedRefreshToken;
import br.com.certamecards.auth.service.LogoutService;
import br.com.certamecards.auth.service.RefreshCookieProperties;
import br.com.certamecards.auth.service.RefreshTokenService;
import br.com.certamecards.auth.service.RotationResult;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class TokenControllerTest {

    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
    private final LogoutService logoutService = mock(LogoutService.class);
    private final RefreshCookieFactory cookieFactory =
            new RefreshCookieFactory(new RefreshCookieProperties("__Host-certame_rt", Duration.ofDays(30), true));
    private final RefreshCookieReader cookieReader =
            new RefreshCookieReader(new RefreshCookieProperties("__Host-certame_rt", Duration.ofDays(30), true));
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenController controller = new TokenController(
            refreshTokenService, accessTokenIssuer, logoutService, cookieFactory, cookieReader, userRepository);

    @Test
    void givenValidRefreshCookie_whenRefreshing_thenRotatesAndReturnsNewAccessToken() {
        User user = new User("ana@exemplo.com", "Ana", UserRole.CANDIDATE);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("__Host-certame_rt", "raw-refresh"));
        when(refreshTokenService.rotate("raw-refresh", null))
                .thenReturn(new RotationResult(user.getId(), new IssuedRefreshToken("new-raw", null)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(accessTokenIssuer.issue(user.getId())).thenReturn(new IssuedAccessToken("access-token", 900));

        ResponseEntity<AuthResponse> response = controller.refresh(request);

        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("__Host-certame_rt");
    }

    @Test
    void givenValidRefreshCookie_whenLoggingOut_thenRevokesToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("__Host-certame_rt", "raw-refresh"));

        controller.logout(request);

        verify(logoutService).logout("raw-refresh");
    }
}
