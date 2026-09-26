package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.ReviewPushResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewPushOutcome(ReviewInsertOutcome reviews, List<UUID> acceptedVoids, StateOutcome states) {

    public ReviewPushResult toResult() {
        return new ReviewPushResult(
                reviews.accepted(),
                reviews.rejected(),
                acceptedVoids,
                states.applied(),
                states.stale(),
                states.ignored());
    }

    public ReviewMetricsInput toMetricsInput(Instant now) {
        return new ReviewMetricsInput(reviews.valid(), reviews.rejected(), acceptedVoids, states, now);
    }
}
