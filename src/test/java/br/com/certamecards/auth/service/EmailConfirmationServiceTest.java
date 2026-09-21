package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.auth.domain.OneTimeToken;
import br.com.certamecards.auth.domain.OneTimeTokenPurpose;
import br.com.certamecards.common.security.ClientOriginProperties;
import br.com.certamecards.mail.SendConfirmationEmailEvent;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class EmailConfirmationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final String EMAIL = "ana@exemplo.com";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OneTimeTokenService oneTimeTokenService = mock(OneTimeTokenService.class);
    private final ClientOriginProperties originProperties =
            new ClientOriginProperties("https://certamecards.localhost", "web");
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final EmailConfirmationService service =
            new EmailConfirmationService(userRepository, oneTimeTokenService, originProperties, eventPublisher, clock);

    @Test
    void givenValidToken_whenConfirming_thenMarksUserEmailVerified() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        OneTimeToken token = new OneTimeToken(
                UUID.randomUUID(), user.getId(), OneTimeTokenPurpose.CONFIRM_EMAIL, "hash", NOW.plusSeconds(60));
        when(oneTimeTokenService.consume("raw-token", OneTimeTokenPurpose.CONFIRM_EMAIL))
                .thenReturn(token);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.confirm("raw-token");

        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    void givenUnverifiedUser_whenResending_thenPublishesConfirmationEvent() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(oneTimeTokenService.issue(user.getId(), OneTimeTokenPurpose.CONFIRM_EMAIL))
                .thenReturn(new IssuedToken("raw-token", NOW.plusSeconds(60)));

        service.resend(EMAIL);

        verify(eventPublisher)
                .publishEvent(org.mockito.ArgumentMatchers.argThat(
                        (SendConfirmationEmailEvent event) -> event.to().equals(EMAIL)));
    }

    @Test
    void givenSecondResendWithinCooldown_whenResending_thenSkipsSendingAgain() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(oneTimeTokenService.issue(any(), any())).thenReturn(new IssuedToken("raw-token", NOW.plusSeconds(60)));

        service.resend(EMAIL);
        service.resend(EMAIL);

        verify(oneTimeTokenService, org.mockito.Mockito.times(1)).issue(any(), any());
    }

    @Test
    void givenAlreadyVerifiedUser_whenResending_thenDoesNotSendEmail() {
        User user = new User(EMAIL, "Ana", UserRole.CANDIDATE);
        user.verifyEmail(NOW.minus(Duration.ofDays(1)));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.resend(EMAIL);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
