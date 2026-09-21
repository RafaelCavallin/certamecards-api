package br.com.certamecards.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.review.domain.ReviewRejectionCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewLogValidatorTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final UUID DECK_ID = UUID.randomUUID();
    private static final UUID DEVICE_ID = UUID.randomUUID();

    private final ReviewLogValidator validator = new ReviewLogValidator();

    @Test
    void givenUnknownCard_whenValidating_thenRejectedAsUnknownCard() {
        UUID cardId = UUID.randomUUID();
        ReviewValidationOutcome outcome = validator.validate(List.of(review(cardId)), Map.of(), FIXED_NOW);

        assertThat(outcome.valid()).isEmpty();
        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.UNKNOWN_CARD.code());
    }

    @Test
    void givenNegativeDuration_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = new ReviewLogInput(
                UUID.randomUUID(), cardId, "review", (short) 3, FIXED_NOW, -1, null, "{}", false, DEVICE_ID, null);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId), FIXED_NOW);

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenReviewKindWithoutRating_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = new ReviewLogInput(
                UUID.randomUUID(), cardId, "review", null, FIXED_NOW, 100, null, "{}", false, DEVICE_ID, null);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId), FIXED_NOW);

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenReviewKindWithRatingOutOfRange_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = new ReviewLogInput(
                UUID.randomUUID(), cardId, "review", (short) 5, FIXED_NOW, 100, null, "{}", false, DEVICE_ID, null);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId), FIXED_NOW);

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenResetKindWithRating_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = new ReviewLogInput(
                UUID.randomUUID(), cardId, "reset", (short) 2, FIXED_NOW, 100, null, "{}", false, DEVICE_ID, null);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId), FIXED_NOW);

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenUnknownKind_whenValidating_thenRejectedAsInvalidReview() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = new ReviewLogInput(
                UUID.randomUUID(), cardId, "bogus", (short) 2, FIXED_NOW, 100, null, "{}", false, DEVICE_ID, null);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId), FIXED_NOW);

        assertThat(outcome.rejected().get(0).code()).isEqualTo(ReviewRejectionCode.INVALID_REVIEW.code());
    }

    @Test
    void givenResetKindWithoutRating_whenValidating_thenIsAccepted() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = new ReviewLogInput(
                UUID.randomUUID(), cardId, "reset", null, FIXED_NOW, 100, null, "{}", false, DEVICE_ID, null);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId), FIXED_NOW);

        assertThat(outcome.rejected()).isEmpty();
        assertThat(outcome.valid()).containsExactly(review);
    }

    @Test
    void givenValidReview_whenValidating_thenIsAccepted() {
        UUID cardId = UUID.randomUUID();
        ReviewLogInput review = review(cardId);
        ReviewValidationOutcome outcome = validator.validate(List.of(review), ownedCards(cardId), FIXED_NOW);

        assertThat(outcome.rejected()).isEmpty();
        assertThat(outcome.valid()).containsExactly(review);
    }

    private Map<UUID, Card> ownedCards(UUID cardId) {
        return Map.of(cardId, new Card(cardId, DECK_ID, "Frente", "Verso"));
    }

    private ReviewLogInput review(UUID cardId) {
        return new ReviewLogInput(
                UUID.randomUUID(), cardId, "review", (short) 3, FIXED_NOW, 4000, null, "{}", false, DEVICE_ID, null);
    }
}
