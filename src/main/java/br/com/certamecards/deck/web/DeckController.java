package br.com.certamecards.deck.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.CreateDeckCommand;
import br.com.certamecards.deck.service.DeckContent;
import br.com.certamecards.deck.service.DeckCreationResult;
import br.com.certamecards.deck.service.DeckProgressResetter;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.deck.service.UpdateDeckCommand;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeckController {

    private final DeckService deckService;
    private final DeckProgressResetter progressResetter;

    public DeckController(DeckService deckService, DeckProgressResetter progressResetter) {
        this.deckService = deckService;
        this.progressResetter = progressResetter;
    }

    @PostMapping("/api/decks")
    public ResponseEntity<DeckResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody CreateDeckRequest request) {
        CreateDeckCommand command = new CreateDeckCommand(
                request.id(),
                principal.id(),
                request.subjectId(),
                new DeckContent(request.name(), request.description()));
        DeckCreationResult result = deckService.create(command);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(DeckResponse.from(result.deck()));
    }

    @PatchMapping("/api/decks/{id}")
    public DeckResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch,
            @Valid @RequestBody UpdateDeckRequest request) {
        UpdateDeckCommand command = new UpdateDeckCommand(
                request.subjectId(), new DeckContent(request.name(), request.description()), ifMatch);
        Deck deck = deckService.update(principal.id(), id, command);
        return DeckResponse.from(deck);
    }

    @DeleteMapping("/api/decks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch) {
        deckService.delete(principal.id(), id, ifMatch);
    }

    @PostMapping("/api/decks/{id}/reset-progress")
    public ResetProgressResponse resetProgress(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        var result = progressResetter.reset(principal.id(), id);
        return new ResetProgressResponse(result.resetCards(), result.cursorHint());
    }
}
