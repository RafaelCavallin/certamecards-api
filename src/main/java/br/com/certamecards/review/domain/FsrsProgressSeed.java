package br.com.certamecards.review.domain;

import java.time.Instant;

public record FsrsProgressSeed(
        CardLearningState state,
        double stability,
        double difficulty,
        Instant due,
        Instant lastReview,
        int reps,
        int lapses,
        int learningSteps,
        int scheduledDays) {}
