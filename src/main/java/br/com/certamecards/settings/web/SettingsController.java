package br.com.certamecards.settings.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.settings.domain.Theme;
import br.com.certamecards.settings.domain.UpdateSettingsCommand;
import br.com.certamecards.settings.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {

    private final UserSettingsService settingsService;

    public SettingsController(UserSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/api/me/settings")
    public UserSettingsResponse get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserSettingsResponse.from(settingsService.get(principal.id()));
    }

    @PutMapping("/api/me/settings")
    public UserSettingsResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody UpdateUserSettingsRequest request) {
        UpdateSettingsCommand command = new UpdateSettingsCommand(
                request.newPerDay(),
                request.reviewsPerDay(),
                request.focusMinutes(),
                request.examDate(),
                request.timeZone(),
                Theme.fromCode(request.theme()));
        return UserSettingsResponse.from(settingsService.update(principal.id(), command));
    }
}
