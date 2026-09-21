package br.com.certamecards.officialdeck.web;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.officialdeck.domain.OfficialDeckAdminFilter;
import br.com.certamecards.officialdeck.persistence.OfficialDeckAdminQuery;
import br.com.certamecards.officialdeck.service.CreateOfficialDeckCommand;
import br.com.certamecards.officialdeck.service.OfficialDeckService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfficialDeckAdminController {

    private final OfficialDeckService deckService;
    private final OfficialDeckAdminQuery adminQuery;

    public OfficialDeckAdminController(OfficialDeckService deckService, OfficialDeckAdminQuery adminQuery) {
        this.deckService = deckService;
        this.adminQuery = adminQuery;
    }

    @GetMapping("/api/admin/official-decks")
    public OfficialDeckAdminPageResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        OfficialDeckAdminFilter filter =
                new OfficialDeckAdminFilter(OfficialDeckStatusParser.parseOptional(status), subjectId, page, size);
        var items = adminQuery.search(filter).stream()
                .map(OfficialDeckAdminSummaryResponse::from)
                .toList();
        return new OfficialDeckAdminPageResponse(items, page, size, adminQuery.count(filter));
    }

    @PostMapping("/api/admin/official-decks")
    @ResponseStatus(HttpStatus.CREATED)
    public OfficialDeckAdminSummaryResponse create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody CreateOfficialDeckRequest request) {
        CreateOfficialDeckCommand command =
                new CreateOfficialDeckCommand(request.id(), request.subjectId(), request.name(), request.description());
        Deck deck = deckService.create(principal.id(), command).deck();
        return summaryOf(deck.getId());
    }

    @GetMapping("/api/admin/official-decks/{id}")
    public OfficialDeckAdminSummaryResponse detail(@PathVariable UUID id) {
        return summaryOf(id);
    }

    private OfficialDeckAdminSummaryResponse summaryOf(UUID deckId) {
        return adminQuery
                .findById(deckId)
                .map(OfficialDeckAdminSummaryResponse::from)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }
}
