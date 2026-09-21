package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class LoginServiceTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final String PASSWORD = "senha-forte";
    private static final String IP = "127.0.0.1";
    private static final String USER_AGENT = "agent";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final LoginThrottle loginThrottle = mock(LoginThrottle.class);
    private final AccessTokenIssuer accessTokenIssuer = mock(AccessTokenIssuer.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final LoginService service = new LoginService(
            userRepository, passwordEncoder, loginThrottle, accessTokenIssuer, refreshTokenService, meterRegistry);

    @Test
    void givenValidCredentialsAndVerifiedEmail_whenLoggingIn_thenIssuesTokens() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        user.changePasswordHash("hashed");
        user.verifyEmail(Instant.parse("2026-09-01T00:00:00Z"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);
        when(accessTokenIssuer.issue(user.getId())).thenReturn(new IssuedAccessToken("access-token", 900));
        when(refreshTokenService.issueNewFamily(user.getId(), USER_AGENT))
                .thenReturn(new IssuedRefreshToken("refresh-token", null));

        LoginResult result = service.login(new LoginCommand(EMAIL, PASSWORD), IP, USER_AGENT);

        assertThat(result.accessToken().token()).isEqualTo("access-token");
        verify(loginThrottle).recordSuccess(EMAIL, IP);
    }

    @Test
    void givenUnknownEmail_whenLoggingIn_thenThrowsInvalidCredentialsAndRecordsFailure() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, PASSWORD), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verify(loginThrottle).recordFailure(EMAIL, IP);
    }

    @Test
    void givenWrongPassword_whenLoggingIn_thenThrowsInvalidCredentials() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        user.changePasswordHash("hashed");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, PASSWORD), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void givenUserRegisteredOnlyByGoogle_whenLoggingInWithPassword_thenThrowsInvalidCredentials() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, PASSWORD), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void givenUnverifiedEmail_whenLoggingIn_thenThrowsEmailNotVerified() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        user.changePasswordHash("hashed");
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(PASSWORD, "hashed")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, PASSWORD), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));
        verify(refreshTokenService, org.mockito.Mockito.never()).issueNewFamily(any(), any());
    }

    @Test
    void givenLockedThrottle_whenLoggingIn_thenPropagatesLoginLocked() {
        org.mockito.Mockito.doThrow(ApiException.of(ErrorCode.LOGIN_LOCKED))
                .when(loginThrottle)
                .ensureAllowed(EMAIL);

        assertThatThrownBy(() -> service.login(new LoginCommand(EMAIL, PASSWORD), IP, USER_AGENT))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.LOGIN_LOCKED));
    }
}
