package br.com.certamecards.review.web;

import br.com.certamecards.review.domain.ReviewSyncLimits;
import br.com.certamecards.review.service.ReviewPushCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ReviewPushRequest(
        @NotNull UUID deviceId,
        @NotNull @Size(max = ReviewSyncLimits.MAX_BATCH_ITEMS) List<@Valid ReviewLogRequest> reviews,
        @NotNull @Size(max = ReviewSyncLimits.MAX_BATCH_ITEMS) List<@Valid ReviewVoidRequest> voids,
        @NotNull @Size(max = ReviewSyncLimits.MAX_BATCH_ITEMS) List<@Valid CardStatePushRequest> states) {

    public ReviewPushCommand toCommand() {
        return new ReviewPushCommand(
                deviceId,
                reviews.stream().map(ReviewLogRequest::toInput).toList(),
                voids.stream().map(ReviewVoidRequest::toInput).toList(),
                states.stream().map(CardStatePushRequest::toInput).toList());
    }
}
