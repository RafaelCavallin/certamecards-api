package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.EmailConfirmationService;
import br.com.certamecards.auth.service.RegisterCommand;
import br.com.certamecards.auth.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistrationController {

    private final RegistrationService registrationService;
    private final EmailConfirmationService emailConfirmationService;

    public RegistrationController(
            RegistrationService registrationService, EmailConfirmationService emailConfirmationService) {
        this.registrationService = registrationService;
        this.emailConfirmationService = emailConfirmationService;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(new RegisterCommand(
                request.email(),
                request.password(),
                request.displayName(),
                request.acceptedTermsVersion(),
                request.timeZone() == null ? "America/Sao_Paulo" : request.timeZone()));
    }

    @PostMapping("/api/auth/confirm-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmEmail(@Valid @RequestBody TokenRequest request) {
        emailConfirmationService.confirm(request.token());
    }

    @PostMapping("/api/auth/resend-confirmation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendConfirmation(@Valid @RequestBody EmailRequest request) {
        emailConfirmationService.resend(request.email());
    }
}
