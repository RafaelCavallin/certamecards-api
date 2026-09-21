package br.com.certamecards.library.web;

import br.com.certamecards.common.config.LibraryProperties;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.library.domain.DeckPreview;
import br.com.certamecards.library.domain.LibraryDeckSummary;
import br.com.certamecards.library.domain.LibraryLimits;
import br.com.certamecards.library.domain.LibraryPage;
import br.com.certamecards.library.domain.LibraryQuery;
import br.com.certamecards.library.domain.SubjectSummary;
import br.com.certamecards.library.service.DeckPreviewService;
import br.com.certamecards.library.service.LibraryCatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class LibraryCatalogController {

    private final LibraryCatalogService catalogService;
    private final DeckPreviewService previewService;
    private final LibraryProperties properties;

    public LibraryCatalogController(
            LibraryCatalogService catalogService, DeckPreviewService previewService, LibraryProperties properties) {
        this.catalogService = catalogService;
        this.previewService = previewService;
        this.properties = properties;
    }

    @GetMapping("/api/library/decks")
    public LibraryPage decks(@AuthenticationPrincipal AuthenticatedUser principal, @Valid LibraryDecksParams params) {
        var pageRequest = params.pageRequest(properties.pageSize());
        return catalogService.search(new LibraryQuery(principal.id(), params.q(), params.subjectId(), pageRequest));
    }

    @GetMapping("/api/library/subjects")
    public ItemsResponse<SubjectSummary> subjects() {
        return new ItemsResponse<>(catalogService.subjectsWithContent());
    }

    @GetMapping("/api/library/suggestions")
    public ItemsResponse<LibraryDeckSummary> suggestions(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) @Min(1) @Max(LibraryLimits.MAX_SUGGESTIONS) Integer limit) {
        int effectiveLimit = limit == null ? properties.suggestions() : limit;
        return new ItemsResponse<>(catalogService.suggestions(principal.id(), effectiveLimit));
    }

    @GetMapping("/api/library/decks/{id}/preview")
    public DeckPreview preview(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return previewService.preview(principal.id(), id);
    }
}
