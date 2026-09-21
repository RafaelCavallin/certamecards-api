package br.com.certamecards.officialdeck.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.officialdeck.service.OfficialCardLifecycleService;
import br.com.certamecards.officialdeck.service.OfficialCardService;
import br.com.certamecards.officialdeck.service.OfficialCardUpdateResult;
import br.com.certamecards.officialdeck.service.UpdateOfficialCardCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfficialCardAdminController {

    private final OfficialCardService cardService;
    private final OfficialCardLifecycleService cardLifecycleService;

    public OfficialCardAdminController(
            OfficialCardService cardService, OfficialCardLifecycleService cardLifecycleService) {
        this.cardService = cardService;
        this.cardLifecycleService = cardLifecycleService;
    }

    @PatchMapping("/api/admin/official-cards/{id}")
    public OfficialCardUpdateResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch,
            @Valid @RequestBody UpdateOfficialCardRequest request) {
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand(
                request.front(), request.back(), request.source(), request.contentChanged(), request.note(), ifMatch);
        OfficialCardUpdateResult result = cardService.update(principal.id(), id, command);
        return OfficialCardUpdateResponse.from(result);
    }

    @DeleteMapping("/api/admin/official-cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch) {
        cardLifecycleService.delete(principal.id(), id, ifMatch);
    }
}
