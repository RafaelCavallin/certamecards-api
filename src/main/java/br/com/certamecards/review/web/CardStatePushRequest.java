package br.com.certamecards.review.web;

import br.com.certamecards.review.domain.CardStateSnapshot;
import br.com.certamecards.review.service.CardStatePushInput;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CardStatePushRequest(
        @NotNull UUID cardId,
        int state,
        double stability,
        double difficulty,
        @NotNull Instant due,
        Instant lastReview,
        int reps,
        int lapses,
        int learningSteps,
        int scheduledDays,
        int reviewCount) {

    public CardStatePushInput toInput() {
        CardStateSnapshot snapshot = new CardStateSnapshot(
                state, stability, difficulty, due, lastReview, reps, lapses, learningSteps, scheduledDays);
        return new CardStatePushInput(cardId, snapshot, reviewCount);
    }
}
