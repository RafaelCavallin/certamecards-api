package br.com.certamecards.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.ClientOriginProperties;
import br.com.certamecards.mail.SendAccountExistsEmailEvent;
import br.com.certamecards.mail.SendConfirmationEmailEvent;
import br.com.certamecards.settings.service.UserSettingsService;
import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import br.com.certamecards.user.persistence.UserRepository;
import br.com.certamecards.user.service.TermsProperties;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class RegistrationServiceTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final String TERMS_VERSION = "2026-01-01";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSettingsService userSettingsService = mock(UserSettingsService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final OneTimeTokenService oneTimeTokenService = mock(OneTimeTokenService.class);
    private final TermsProperties termsProperties = new TermsProperties(TERMS_VERSION);
    private final ClientOriginProperties originProperties =
            new ClientOriginProperties("https://certamecards.localhost", "web");
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final RegistrationService service = new RegistrationService(
            userRepository,
            userSettingsService,
            passwordEncoder,
            oneTimeTokenService,
            termsProperties,
            originProperties,
            eventPublisher);

    @Test
    void givenOutdatedTermsVersion_whenRegistering_thenThrowsValidationFailed() {
        RegisterCommand command = new RegisterCommand(EMAIL, "senha-forte", "Ana", "old-version", "UTC");

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void givenEmailAlreadyRegistered_whenRegistering_thenSendsAccountExistsEventWithoutCreatingUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new User(EMAIL, "Ana", UserRole.CANDIDATE)));
        RegisterCommand command = new RegisterCommand(EMAIL, "senha-forte", "Ana", TERMS_VERSION, "UTC");

        service.register(command);

        verify(eventPublisher).publishEvent(new SendAccountExistsEmailEvent(EMAIL));
        verify(userRepository, never()).save(any());
    }

    @Test
    void givenNewEmail_whenRegistering_thenCreatesUserAndSendsConfirmationEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha-forte")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(oneTimeTokenService.issue(any(), any())).thenReturn(new IssuedToken("raw-token", Instant.now()));
        RegisterCommand command = new RegisterCommand(EMAIL, "senha-forte", "Ana", TERMS_VERSION, "UTC");

        service.register(command);

        verify(userSettingsService).createDefault(any(), org.mockito.ArgumentMatchers.eq("UTC"));
        verify(eventPublisher)
                .publishEvent(org.mockito.ArgumentMatchers.argThat((SendConfirmationEmailEvent event) ->
                        event.to().equals(EMAIL) && event.link().contains("raw-token")));
    }
}
