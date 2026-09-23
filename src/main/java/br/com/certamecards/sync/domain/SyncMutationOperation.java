package br.com.certamecards.sync.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record SyncMutationOperation(
        UUID operationId,
        SyncOperationKind kind,
        UUID entityId,
        UUID parentId,
        Integer baseVersion,
        UUID predecessorOperationId,
        List<UUID> dependsOn,
        Instant occurredAt,
        EventClock clock,
        Instant observedServerTime,
        JsonNode payload) {}
