package br.com.certamecards.auth.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.auth.service.EmailConfirmationService;
import br.com.certamecards.auth.service.RegisterCommand;
import br.com.certamecards.auth.service.RegistrationService;
import org.junit.jupiter.api.Test;

class RegistrationControllerTest {

    private final RegistrationService registrationService = mock(RegistrationService.class);
    private final EmailConfirmationService emailConfirmationService = mock(EmailConfirmationService.class);
    private final RegistrationController controller =
            new RegistrationController(registrationService, emailConfirmationService);

    @Test
    void givenRequestWithoutTimeZone_whenRegistering_thenDefaultsToSaoPaulo() {
        RegisterRequest request = new RegisterRequest("ana@exemplo.com", "senha-forte", "Ana", "2026-09-01", null);

        controller.register(request);

        verify(registrationService)
                .register(new RegisterCommand(
                        "ana@exemplo.com", "senha-forte", "Ana", "2026-09-01", "America/Sao_Paulo"));
    }

    @Test
    void givenRequestWithTimeZone_whenRegistering_thenUsesProvidedTimeZone() {
        RegisterRequest request = new RegisterRequest("ana@exemplo.com", "senha-forte", "Ana", "2026-09-01", "UTC");

        controller.register(request);

        verify(registrationService)
                .register(new RegisterCommand("ana@exemplo.com", "senha-forte", "Ana", "2026-09-01", "UTC"));
    }

    @Test
    void givenToken_whenConfirmingEmail_thenDelegatesToService() {
        controller.confirmEmail(new TokenRequest("raw-token"));

        verify(emailConfirmationService).confirm("raw-token");
    }

    @Test
    void givenEmail_whenResendingConfirmation_thenDelegatesToService() {
        controller.resendConfirmation(new EmailRequest("ana@exemplo.com"));

        verify(emailConfirmationService).resend(eq("ana@exemplo.com"));
    }
}
