package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.UUID;

public record CardStateChange(
        UUID cardId,
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
        long changeSeq) {}
