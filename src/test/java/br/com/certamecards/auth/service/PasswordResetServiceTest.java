package br.com.certamecards.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.common.security.ClientOriginProperties;
import br.com.certamecards.mail.SendPasswordResetEmailEvent;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OneTimeTokenService oneTimeTokenService = mock(OneTimeTokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final ClientOriginProperties originProperties =
            new ClientOriginProperties("https://certamecards.localhost", "web");
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final PasswordResetService service = new PasswordResetService(
            userRepository,
            oneTimeTokenService,
            passwordEncoder,
            refreshTokenService,
            originProperties,
            eventPublisher);

    @Test
    void givenKnownEmail_whenRequestingForgotPassword_thenPublishesResetEvent() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(oneTimeTokenService.issue(user.getId(), OneTimeTokenPurpose.RESET_PASSWORD))
                .thenReturn(new IssuedToken("raw-token", NOW.plusSeconds(60)));

        service.forgot(EMAIL);

        verify(eventPublisher)
                .publishEvent(org.mockito.ArgumentMatchers.argThat(
                        (SendPasswordResetEmailEvent event) -> event.to().equals(EMAIL)));
    }

    @Test
    void givenUnknownEmail_whenRequestingForgotPassword_thenDoesNotPublishEvent() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        service.forgot(EMAIL);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void givenValidResetToken_whenResetting_thenChangesPasswordAndRevokesAllRefreshTokens() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(), user.getId(), OneTimeTokenPurpose.RESET_PASSWORD, "hash", NOW.plusSeconds(60));
        when(oneTimeTokenService.consume("raw-token", OneTimeTokenPurpose.RESET_PASSWORD))
                .thenReturn(token);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nova-senha")).thenReturn("hashed-nova-senha");

        service.reset(new ResetPasswordCommand("raw-token", "nova-senha"));

        verify(refreshTokenService).revokeAllForUser(user.getId());
    }
}
