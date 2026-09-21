package br.com.certamecards.card.web;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardContent;
import br.com.certamecards.card.service.CardCreationResult;
import br.com.certamecards.card.service.CardService;
import br.com.certamecards.card.service.CreateCardCommand;
import br.com.certamecards.card.service.UpdateCardCommand;
import br.com.certamecards.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/api/decks/{deckId}/cards")
    public ResponseEntity<CardResponse> create(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID deckId,
            @Valid @RequestBody CreateCardRequest request) {
        CreateCardCommand command = new CreateCardCommand(
                request.id(),
                deckId,
                principal.id(),
                new CardContent(request.front(), request.back(), request.source()));
        CardCreationResult result = cardService.create(command);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(CardResponse.from(result.card()));
    }

    @PatchMapping("/api/cards/{id}")
    public CardResponse update(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch,
            @Valid @RequestBody UpdateCardRequest request) {
        UpdateCardCommand command =
                new UpdateCardCommand(new CardContent(request.front(), request.back(), request.source()), ifMatch);
        Card card = cardService.update(principal.id(), id, command);
        return CardResponse.from(card);
    }

    @DeleteMapping("/api/cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestHeader("If-Match") int ifMatch) {
        cardService.delete(principal.id(), id, ifMatch);
    }

    @PutMapping("/api/cards/{id}/suspension")
    public CardStateResponse suspension(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestBody SuspensionRequest request) {
        return CardStateResponse.from(cardService.setSuspension(principal.id(), id, request.suspended()));
    }
}
