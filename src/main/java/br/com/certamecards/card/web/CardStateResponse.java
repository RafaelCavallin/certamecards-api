package br.com.certamecards.card.web;

import br.com.certamecards.review.domain.CardState;
import java.time.Instant;

public record CardStateResponse(
        String cardId,
        int state,
        double stability,
        double difficulty,
        Instant due,
        Instant lastReview,
        int reps,
        int lapses,
        int learningSteps,
        int scheduledDays,
        int reviewCount,
        boolean suspended,
        String contentUpdateNote,
        Instant contentUpdatedAt,
        Instant updatedAt,
        Long changeSeq) {

    public static CardStateResponse from(CardState state) {
        var progress = state.getProgress();
        return new CardStateResponse(
                state.getId().getCardId().toString(),
                progress.getState().code(),
                progress.getStability(),
                progress.getDifficulty(),
                progress.getDue(),
                progress.getLastReview(),
                progress.getReps(),
                progress.getLapses(),
                progress.getLearningSteps(),
                progress.getScheduledDays(),
                state.getReviewCount(),
                state.isSuspended(),
                state.getContentUpdateNote(),
                state.getContentUpdatedAt(),
                state.getUpdatedAt(),
                state.getChangeSeq());
    }
}
