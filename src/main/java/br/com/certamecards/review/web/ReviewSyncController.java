package br.com.certamecards.review.web;

import br.com.certamecards.common.security.AuthenticatedUser;
import br.com.certamecards.review.domain.CardReviewHistory;
import br.com.certamecards.review.domain.ReviewPushResult;
import br.com.certamecards.review.service.ReviewHistoryService;
import br.com.certamecards.review.service.ReviewSyncService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewSyncController {

    private final ReviewSyncService reviewSyncService;
    private final ReviewHistoryService reviewHistoryService;

    public ReviewSyncController(ReviewSyncService reviewSyncService, ReviewHistoryService reviewHistoryService) {
        this.reviewSyncService = reviewSyncService;
        this.reviewHistoryService = reviewHistoryService;
    }

    @PostMapping("/api/sync/reviews")
    public ReviewPushResult push(
            @AuthenticationPrincipal AuthenticatedUser principal, @Valid @RequestBody ReviewPushRequest request) {
        return reviewSyncService.push(principal.id(), request.toCommand());
    }

    @GetMapping("/api/cards/{id}/reviews")
    public CardReviewHistory reviews(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return reviewHistoryService.historyForCard(principal.id(), id);
    }
}
