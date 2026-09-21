package br.com.certamecards.auth.web;

import br.com.certamecards.auth.service.PasswordResetService;
import br.com.certamecards.auth.service.ResetPasswordCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordController {

    private final PasswordResetService passwordResetService;

    public PasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/api/auth/password/forgot")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgot(@Valid @RequestBody EmailRequest request) {
        passwordResetService.forgot(request.email());
    }

    @PostMapping("/api/auth/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(new ResetPasswordCommand(request.token(), request.newPassword()));
    }
}
