package br.com.certamecards.officialdeck.web;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.card.web.CardResponse;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.officialdeck.service.CreateOfficialCardCommand;
import br.com.certamecards.officialdeck.service.OfficialCardLifecycleService;
import br.com.certamecards.officialdeck.service.OfficialDeckService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class OfficialDeckCardsController {

    private final OfficialDeckService deckService;
    private final OfficialCardLifecycleService cardLifecycleService;
    private final CardRepository cardRepository;

    public OfficialDeckCardsController(
            OfficialDeckService deckService,
            OfficialCardLifecycleService cardLifecycleService,
            CardRepository cardRepository) {
        this.deckService = deckService;
        this.cardLifecycleService = cardLifecycleService;
        this.cardRepository = cardRepository;
    }

    @GetMapping("/api/admin/official-decks/{id}/cards")
    public OfficialCardPageResponse listCards(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        deckService.findOfficial(id);
        Page<Card> result = cardRepository.findByDeckIdAndAudit_DeletedAtIsNullOrderById(
                id, PageRequest.of(page, size, Sort.unsorted()));
        var items = result.getContent().stream().map(CardResponse::from).toList();
        return new OfficialCardPageResponse(items, page, size, result.getTotalElements());
    }

    @PostMapping("/api/admin/official-decks/{id}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse createCard(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateOfficialCardRequest request) {
        CreateOfficialCardCommand command =
                new CreateOfficialCardCommand(request.id(), id, request.front(), request.back(), request.source());
        return CardResponse.from(
                cardLifecycleService.create(principal.id(), command).card());
    }
}
