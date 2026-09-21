package br.com.certamecards.library.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.library.service.DeckDuplicationService;
import br.com.certamecards.library.service.DuplicationResult;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeckDuplicationController {

    private final DeckDuplicationService duplicationService;

    public DeckDuplicationController(DeckDuplicationService duplicationService) {
        this.duplicationService = duplicationService;
    }

    @PostMapping("/api/library/decks/{id}/duplicate")
    public ResponseEntity<DuplicateDeckResponse> duplicate(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody DuplicateDeckRequest request) {
        DuplicationResult result = duplicationService.duplicate(request.toCommand(principal.id(), id));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(DuplicateDeckResponse.from(result));
    }
}
