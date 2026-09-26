package br.com.certamecards.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.review.domain.ReviewRejectionCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewLogValidatorTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final UUID DECK_ID = UUID.randomUUID();
    private static final UUID DEVICE_ID = UUID.randomUUID();

    private final ReviewLogValidator validator = new ReviewLogValidator();

    @Test
    void givenUnknownCard_whenValidating_thenRejectedAsUnknownCard() {
        UUID cardId = UUID.randomUUID();
        ReviewValidationOutcome outcome = validator.validate(List.of(review(cardId)), Map.of());

        assertThat(outcome.valid()).isEmpty();
        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.UNKNOWN_CARD.code());
    }

    @Test
    @DisplayName("TU-28 — durationMs fora da faixa é rejeitado como invalid_review")
    void givenNegativeDuration_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = reviewInput(cardId, "review", (short) 3, -1);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId));

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenReviewKindWithoutRating_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = reviewInput(cardId, "review", null, 100);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId));

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenReviewKindWithRatingOutOfRange_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = reviewInput(cardId, "review", (short) 5, 100);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId));

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenResetKindWithRating_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = reviewInput(cardId, "reset", (short) 2, 100);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId));

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenUnknownKind_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = reviewInput(cardId, "bogus", (short) 2, 100);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId));

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenResetKindWithoutRating_whenValidating_thenIsAccepted() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = reviewInput(cardId, "reset", null, 100);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId));

        assertThat(outcome.rejected()).isEmpty();
        assertThat(outcome.valid()).containsExactly(review);
    }

    @Test
    void givenValidReview_whenValidating_thenIsAccepted() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = review(cardId);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId));

        assertThat(outcome.rejected()).isEmpty();
        assertThat(outcome.valid()).containsExactly(review);
    }

    private Map<UUID, Card> ownedCards(UUID cardId) {
        return Map.of(cardId, new Card(cardId, DECK_ID, "Frente", "Verso"));
    }

    private ReviewLogInput review(UUID cardId) {
        return reviewInput(cardId, "review", (short) 3, 4000);
    }

    private ReviewLogInput reviewInput(UUID cardId, String kind, Short rating, int durationMs) {
        return new ReviewLogInput(
                UUID.randomUUID(),
                cardId,
                kind,
                rating,
                FIXED_NOW,
                durationMs,
                null,
                "{}",
                false,
                DEVICE_ID,
                null,
                new EventClock(FIXED_NOW, 0),
                FIXED_NOW);
    }
}
