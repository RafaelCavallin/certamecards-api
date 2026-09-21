package br.com.certamecards.library.web;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.library.service.DeckContentService;
import br.com.certamecards.library.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final DeckContentService contentService;
    private final LibraryProperties properties;

    public SubscriptionController(
            SubscriptionService subscriptionService, DeckContentService contentService, LibraryProperties properties) {
        this.subscriptionService = subscriptionService;
        this.contentService = contentService;
        this.properties = properties;
    }

    @PostMapping("/api/library/decks/{id}/subscription")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscribeResponse subscribe(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return SubscribeResponse.from(subscriptionService.subscribe(principal.id(), id));
    }

    @DeleteMapping("/api/library/decks/{id}/subscription")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        subscriptionService.cancel(principal.id(), id);
    }

    @GetMapping("/api/library/decks/{id}/content")
    public DeckContentResponse content(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id, @Valid ContentParams params) {
        var cursor = params.cursor(properties.contentPageSize());
        return DeckContentResponse.from(contentService.content(principal.id(), id, cursor));
    }
}
