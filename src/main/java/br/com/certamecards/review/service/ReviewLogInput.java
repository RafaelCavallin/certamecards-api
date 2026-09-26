package br.com.certamecards.review.service;

import br.com.certamecards.common.sync.EventClock;
import java.time.Instant;
import java.util.UUID;

public record ReviewLogInput(
        UUID id,
        UUID cardId,
        String kind,
        Short rating,
        Instant reviewedAt,
        int durationMs,
        String stateBefore,
        String stateAfter,
        boolean offline,
        UUID deviceId,
        UUID sessionId,
        EventClock clock,
        Instant observedServerTime) {}
