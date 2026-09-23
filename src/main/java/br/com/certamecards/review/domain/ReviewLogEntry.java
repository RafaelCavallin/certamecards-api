package br.com.certamecards.review.domain;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.util.RawValue;

public record ReviewLogEntry(
        UUID id,
        UUID cardId,
        String kind,
        Short rating,
        Instant reviewedAt,
        int durationMs,
        RawValue stateBefore,
        RawValue stateAfter,
        boolean offline,
        UUID deviceId,
        UUID sessionId,
        long changeSeq,
        Instant eventAt,
        int eventCounter,
        UUID eventDeviceId,
        UUID operationId) {}
