package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.sync.domain.SyncMutationBatch;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class MutationBatchValidatorTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private final MutationBatchValidator validator = new MutationBatchValidator();

    @Test
    void givenValidBatch_whenValidating_thenDoesNotThrow() {
        SyncMutationBatch batch = new SyncMutationBatch(UUID.randomUUID(), List.of(operation()));

        validator.validate(batch);
    }

    @Test
    void givenNullDeviceId_whenValidating_thenRejects() {
        SyncMutationBatch batch = new SyncMutationBatch(null, List.of(operation()));

        assertRejected(batch);
    }

    @Test
    void givenNullOperations_whenValidating_thenRejects() {
        SyncMutationBatch batch = new SyncMutationBatch(UUID.randomUUID(), null);

        assertRejected(batch);
    }

    @Test
    void givenEmptyOperations_whenValidating_thenRejects() {
        SyncMutationBatch batch = new SyncMutationBatch(UUID.randomUUID(), List.of());

        assertRejected(batch);
    }

    @Test
    void givenTooManyOperations_whenValidating_thenRejects() {
        List<SyncMutationOperation> operations =
                IntStream.range(0, 101).mapToObj(index -> operation()).toList();
        SyncMutationBatch batch = new SyncMutationBatch(UUID.randomUUID(), operations);

        assertRejected(batch);
    }

    @Test
    void givenOperationWithoutOperationId_whenValidating_thenRejects() {
        assertRejected(batchOf(operation(null, UUID.randomUUID(), List.of())));
    }

    @Test
    void givenOperationWithTooManyDependencies_whenValidating_thenRejects() {
        List<UUID> dependsOn =
                IntStream.range(0, 11).mapToObj(index -> UUID.randomUUID()).toList();
        assertRejected(batchOf(operation(UUID.randomUUID(), UUID.randomUUID(), dependsOn)));
    }

    @Test
    void givenOperationWithoutKind_whenValidating_thenRejects() {
        SyncMutationOperation invalid = new SyncMutationOperation(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());

        assertRejected(batchOf(invalid));
    }

    @Test
    void givenOperationWithoutEntityId_whenValidating_thenRejects() {
        SyncMutationOperation invalid = new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_CREATE,
                null,
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());

        assertRejected(batchOf(invalid));
    }

    @Test
    void givenOperationWithNullDependsOn_whenValidating_thenRejects() {
        SyncMutationOperation invalid = new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_CREATE,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());

        assertRejected(batchOf(invalid));
    }

    @Test
    void givenOperationWithoutClockWallTime_whenValidating_thenRejects() {
        SyncMutationOperation invalid = new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_CREATE,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(null, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());

        assertRejected(batchOf(invalid));
    }

    @Test
    void givenOperationWithoutClock_whenValidating_thenRejects() {
        SyncMutationOperation invalid = new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_CREATE,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                null,
                NOW,
                JsonNodeFactory.instance.objectNode());

        assertRejected(batchOf(invalid));
    }

    @Test
    void givenOperationWithoutPayload_whenValidating_thenRejects() {
        SyncMutationOperation invalid = new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_CREATE,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                null);

        assertRejected(batchOf(invalid));
    }

    private void assertRejected(SyncMutationBatch batch) {
        assertThatThrownBy(() -> validator.validate(batch)).isInstanceOf(ApiException.class);
    }

    private SyncMutationBatch batchOf(SyncMutationOperation operation) {
        return new SyncMutationBatch(UUID.randomUUID(), List.of(operation));
    }

    private SyncMutationOperation operation() {
        return operation(UUID.randomUUID(), UUID.randomUUID(), List.of());
    }

    private SyncMutationOperation operation(UUID operationId, UUID entityId, List<UUID> dependsOn) {
        return new SyncMutationOperation(
                operationId,
                SyncOperationKind.DECK_CREATE,
                entityId,
                null,
                null,
                null,
                dependsOn,
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }
}
