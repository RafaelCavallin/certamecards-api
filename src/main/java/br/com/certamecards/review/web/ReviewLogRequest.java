package br.com.certamecards.review.web;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.review.service.ReviewLogInput;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record ReviewLogRequest(
        @NotNull UUID id,
        @NotNull UUID cardId,
        @NotNull String kind,
        Short rating,
        @NotNull Instant reviewedAt,
        int durationMs,
        JsonNode stateBefore,
        @NotNull JsonNode stateAfter,
        boolean offline,
        @NotNull UUID deviceId,
        UUID sessionId,
        @NotNull EventClock clock,
        @NotNull Instant observedServerTime) {

    public ReviewLogInput toInput() {
        return new ReviewLogInput(
                id,
                cardId,
                kind,
                rating,
                reviewedAt,
                durationMs,
                stateBefore == null ? null : stateBefore.toString(),
                stateAfter.toString(),
                offline,
                deviceId,
                sessionId,
                clock,
                observedServerTime);
    }
}
