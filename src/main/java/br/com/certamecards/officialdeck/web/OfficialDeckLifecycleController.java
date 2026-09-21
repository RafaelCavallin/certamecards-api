package br.com.certamecards.officialdeck.web;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.officialdeck.persistence.OfficialDeckAdminQuery;
import br.com.certamecards.officialdeck.service.OfficialDeckService;
import br.com.certamecards.officialdeck.service.OfficialDeckStatusService;
import br.com.certamecards.officialdeck.service.UpdateOfficialDeckCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfficialDeckLifecycleController {

    private final OfficialDeckService deckService;
    private final OfficialDeckStatusService statusService;
    private final OfficialDeckAdminQuery adminQuery;

    public OfficialDeckLifecycleController(
            OfficialDeckService deckService,
            OfficialDeckStatusService statusService,
            OfficialDeckAdminQuery adminQuery) {
        this.deckService = deckService;
        this.statusService = statusService;
        this.adminQuery = adminQuery;
    }

    @PatchMapping("/api/admin/official-decks/{id}")
    public OfficialDeckAdminSummaryResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch,
            @Valid @RequestBody UpdateOfficialDeckRequest request) {
        UpdateOfficialDeckCommand command =
                new UpdateOfficialDeckCommand(request.subjectId(), request.name(), request.description(), ifMatch);
        Deck deck = deckService.update(principal.id(), id, command);
        return summaryOf(deck.getId());
    }

    @DeleteMapping("/api/admin/official-decks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch) {
        statusService.delete(principal.id(), id, ifMatch);
    }

    @PutMapping("/api/admin/official-decks/{id}/status")
    public OfficialDeckAdminSummaryResponse changeStatus(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch,
            @Valid @RequestBody ChangeOfficialDeckStatusRequest request) {
        var target = OfficialDeckStatusParser.parseRequired(request.status());
        Deck deck = statusService.changeStatus(principal.id(), id, target, ifMatch);
        return summaryOf(deck.getId());
    }

    private OfficialDeckAdminSummaryResponse summaryOf(UUID deckId) {
        return adminQuery
                .findById(deckId)
                .map(OfficialDeckAdminSummaryResponse::from)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }
}
