package br.com.certamecards.review.domain;

import java.time.Instant;

public record CardStateSnapshot(
        int state,
        double stability,
        double difficulty,
        Instant due,
        Instant lastReview,
        int reps,
        int lapses,
        int learningSteps,
        int scheduledDays) {

    public static CardStateSnapshot from(FsrsProgress progress) {
        return new CardStateSnapshot(
                (int) progress.getState().code(),
                progress.getStability(),
                progress.getDifficulty(),
                progress.getDue(),
                progress.getLastReview(),
                progress.getReps(),
                progress.getLapses(),
                progress.getLearningSteps(),
                progress.getScheduledDays());
    }
}
