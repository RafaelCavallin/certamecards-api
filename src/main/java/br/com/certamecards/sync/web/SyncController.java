package br.com.certamecards.sync.web;

import br.com.certamecards.common.config.SyncProperties;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.sync.domain.ChangesPage;
import br.com.certamecards.sync.domain.SyncLimits;
import br.com.certamecards.sync.service.SyncChangesService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class SyncController {

    private final SyncChangesService syncChangesService;
    private final SyncProperties properties;

    public SyncController(SyncChangesService syncChangesService, SyncProperties properties) {
        this.syncChangesService = syncChangesService;
        this.properties = properties;
    }

    @GetMapping("/api/sync/changes")
    public ChangesPage changes(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(defaultValue = "0") @Min(0) long cursor,
            @RequestParam(required = false) @Min(SyncLimits.MIN_PAGE_LIMIT) @Max(SyncLimits.MAX_PAGE_LIMIT)
                    Integer limit) {
        int effectiveLimit = limit == null ? properties.defaultPageLimit() : limit;
        return syncChangesService.changesSince(principal.id(), cursor, effectiveLimit);
    }
}
