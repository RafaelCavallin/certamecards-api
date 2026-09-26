package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class SyncMutationMetricsRecorderTest {

    @Test
    void recordsOnlyKindAndOutcomeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        UUID operationId = UUID.randomUUID();
        SyncMutationOperation operation = new SyncMutationOperation(
                operationId,
                SyncOperationKind.DECK_CREATE,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                Instant.parse("2026-09-24T12:00:00Z"),
                new EventClock(Instant.parse("2026-09-24T12:00:00Z"), 0),
                Instant.parse("2026-09-24T12:00:00Z"),
                JsonNodeFactory.instance.objectNode());
        MutationResult result = new MutationResult(
                operationId,
                MutationOutcome.APPLIED,
                1,
                1L,
                new EventOrder(Instant.parse("2026-09-24T12:00:00Z"), 0, UUID.randomUUID(), operationId),
                null,
                null);

        new SyncMutationMetricsRecorder(registry)
                .record(List.of(operation), List.of(result), Instant.parse("2026-09-24T12:01:00Z"));

        assertThat(registry.find("sync.mutations.items").counter().count()).isEqualTo(1);
        assertThat(registry.find("sync.mutations.lag.seconds").summary().count())
                .isEqualTo(1);
    }
}
