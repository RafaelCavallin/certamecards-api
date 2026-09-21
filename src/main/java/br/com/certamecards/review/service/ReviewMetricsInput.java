package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.RejectedReview;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewMetricsInput(
        List<ReviewLogInput> acceptedReviews,
        List<RejectedReview> rejectedReviews,
        List<UUID> acceptedVoids,
        StateOutcome stateOutcome,
        Instant receivedAt) {}
