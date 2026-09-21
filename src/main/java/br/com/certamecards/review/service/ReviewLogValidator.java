package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.review.domain.RejectedReview;
import br.com.certamecards.review.domain.ReviewKind;
import br.com.certamecards.review.domain.ReviewRejectionCode;
import br.com.certamecards.review.domain.ReviewSyncLimits;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReviewLogValidator {

    public ReviewValidationOutcome validate(List<ReviewLogInput> reviews, Map<UUID, Card> ownedCards, Instant now) {
        List<ReviewLogInput> valid = new ArrayList<>();
        List<RejectedReview> rejected = new ArrayList<>();
        for (ReviewLogInput review : reviews) {
            ReviewRejectionCode code = rejectionCodeFor(review, ownedCards, now);
            addOutcome(review, code, valid, rejected);
        }
        return new ReviewValidationOutcome(valid, rejected);
    }

    private void addOutcome(
            ReviewLogInput review,
            ReviewRejectionCode code,
            List<ReviewLogInput> valid,
            List<RejectedReview> rejected) {
        if (code == null) {
            valid.add(review);
            return;
        }
        rejected.add(RejectedReview.of(review.id(), code));
    }

    private ReviewRejectionCode rejectionCodeFor(ReviewLogInput review, Map<UUID, Card> ownedCards, Instant now) {
        if (!ownedCards.containsKey(review.cardId())) {
            return ReviewRejectionCode.UNKNOWN_CARD;
        }
        if (review.reviewedAt().isAfter(now.plus(ReviewSyncLimits.FUTURE_TOLERANCE))) {
            return ReviewRejectionCode.FUTURE_TIMESTAMP;
        }
        if (isInvalid(review)) {
            return ReviewRejectionCode.INVALID_REVIEW;
        }
        return null;
    }

    private boolean isInvalid(ReviewLogInput review) {
        boolean durationOutOfRange = review.durationMs() < ReviewSyncLimits.MIN_DURATION_MS
                || review.durationMs() > ReviewSyncLimits.MAX_DURATION_MS;
        return durationOutOfRange || !hasValidKindAndRating(review);
    }

    private boolean hasValidKindAndRating(ReviewLogInput review) {
        ReviewKind kind = parseKind(review.kind());
        if (kind == null) {
            return false;
        }
        if (kind == ReviewKind.REVIEW) {
            return review.rating() != null
                    && review.rating() >= ReviewSyncLimits.MIN_RATING
                    && review.rating() <= ReviewSyncLimits.MAX_RATING;
        }
        return review.rating() == null;
    }

    private ReviewKind parseKind(String kind) {
        try {
            return ReviewKind.fromCode(kind);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
