package br.com.certamecards.review.domain;

import java.util.List;
import java.util.UUID;

public record ReviewPushResult(
        List<UUID> acceptedReviewIds,
        List<RejectedReview> rejectedReviews,
        List<UUID> acceptedVoids,
        List<AppliedState> appliedStates,
        List<StaleState> staleStates,
        List<IgnoredState> ignoredStates) {}
