package br.com.certamecards.user.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.user.service.AccountService;
import br.com.certamecards.user.service.DeleteAccountCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/api/me")
    public MeResponse getProfile(@AuthenticationPrincipal AuthenticatedUser principal) {
        return MeResponse.from(accountService.getProfile(principal.id()));
    }

    @PatchMapping("/api/me")
    public MeResponse updateDisplayName(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateDisplayNameRequest request) {
        return MeResponse.from(accountService.updateDisplayName(principal.id(), request.displayName()));
    }

    @PostMapping("/api/me/terms")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptTerms(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody AcceptTermsRequest request) {
        accountService.acceptTerms(principal.id(), request.version());
    }

    @DeleteMapping("/api/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @AuthenticationPrincipal AuthenticatedUser principal, @RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(
                principal.id(), new DeleteAccountCommand(request.password(), request.reauthToken()));
    }
}
