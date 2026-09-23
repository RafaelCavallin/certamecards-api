package br.com.certamecards.review.web;

import br.com.certamecards.common.config.SyncProperties;
import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.review.domain.CardReviewHistory;
import br.com.certamecards.review.domain.ReviewPushResult;
import br.com.certamecards.review.service.ReviewHistoryService;
import br.com.certamecards.review.service.ReviewSyncService;
import br.com.certamecards.sync.domain.SyncLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ReviewSyncController {

    private final ReviewSyncService reviewSyncService;
    private final ReviewHistoryService reviewHistoryService;
    private final SyncProperties properties;

    public ReviewSyncController(
            ReviewSyncService reviewSyncService, ReviewHistoryService reviewHistoryService, SyncProperties properties) {
        this.reviewSyncService = reviewSyncService;
        this.reviewHistoryService = reviewHistoryService;
        this.properties = properties;
    }

    @PostMapping("/api/sync/reviews")
    public ReviewPushResult push(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody ReviewPushRequest request) {
        return reviewSyncService.push(principal.id(), request.toCommand());
    }

    @GetMapping("/api/cards/{id}/reviews")
    public CardReviewHistory reviews(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID id,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) @Min(SyncLimits.MIN_PAGE_LIMIT) @Max(SyncLimits.MAX_PAGE_LIMIT)
                    Integer limit) {
        int effectiveLimit = limit == null ? properties.defaultPageLimit() : limit;
        return reviewHistoryService.historyForCard(principal.id(), id, cursor, effectiveLimit);
    }
}
