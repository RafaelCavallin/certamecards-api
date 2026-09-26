package br.com.certamecards.sync.web;

import br.com.certamecards.common.config.SyncProperties;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.sync.domain.ConflictDetail;
import br.com.certamecards.sync.domain.ConflictListPage;
import br.com.certamecards.sync.service.SyncConflictService;
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
public class SyncConflictController {

    private static final int MAX_CONFLICT_PAGE_LIMIT = 100;
    private static final int MIN_CONFLICT_PAGE_LIMIT = 1;

    private final SyncConflictService conflictService;
    private final SyncProperties properties;

    public SyncConflictController(SyncConflictService conflictService, SyncProperties properties) {
        this.conflictService = conflictService;
        this.properties = properties;
    }

    @GetMapping("/api/sync/conflicts")
    public ConflictListPage list(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @Min(MIN_CONFLICT_PAGE_LIMIT) @Max(MAX_CONFLICT_PAGE_LIMIT) Integer limit) {
        int effectiveLimit = limit == null ? properties.defaultPageLimit() : limit;
        return conflictService.list(principal.id(), cursor, Math.min(effectiveLimit, MAX_CONFLICT_PAGE_LIMIT));
    }

    @GetMapping("/api/sync/conflicts/{id}")
    public ConflictDetail detail(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return conflictService.detail(principal.id(), id);
    }
}
