package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.common.sync.EventOrderNormalizer;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationBatch;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncMutationResponse;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

class MutationBatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DEVICE_ID = UUID.randomUUID();
    private final MutationBatchValidator validator = mock(MutationBatchValidator.class);
    private final MutationDependencyResolver dependencyResolver = mock(MutationDependencyResolver.class);
    private final EventOrderNormalizer orderNormalizer = mock(EventOrderNormalizer.class);
    private final EntityMutationCoordinator coordinator = mock(EntityMutationCoordinator.class);
    private final PreferenceMutationCoordinator preferenceCoordinator = mock(PreferenceMutationCoordinator.class);
    private final MutationHandlerRegistry handlers = mock(MutationHandlerRegistry.class);
    private final MutationReceiptService receipts = mock(MutationReceiptService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final SyncMutationMetricsRecorder metrics = mock(SyncMutationMetricsRecorder.class);
    private final MutationBatchService service = new MutationBatchService(
            validator,
            dependencyResolver,
            orderNormalizer,
            coordinator,
            preferenceCoordinator,
            handlers,
            receipts,
            new ObjectMapper(),
            clock,
            metrics);

    @Test
    void givenDependencyThatFailed_whenApplying_thenDependentIsBlocked() {
        SyncMutationOperation parent = operation(SyncOperationKind.DECK_CREATE, List.of());
        SyncMutationOperation child = operation(SyncOperationKind.CARD_CREATE, List.of(parent.operationId()));
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(parent, child));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(parent, child));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(parent)).thenReturn(true);
        when(coordinator.execute(any()))
                .thenReturn(new MutationResult(
                        parent.operationId(),
                        MutationOutcome.ACTION_REQUIRED,
                        null,
                        null,
                        order(),
                        null,
                        new br.com.certamecards.sync.domain.MutationError("validation_failed", "erro")));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        MutationResult childResult = response.results().stream()
                .filter(result -> result.operationId().equals(child.operationId()))
                .findFirst()
                .orElseThrow();
        assertThat(childResult.outcome()).isEqualTo(MutationOutcome.DEPENDENCY_BLOCKED);
        assertThat(childResult.error().code()).isEqualTo("dependency_failed");
    }

    @Test
    @DisplayName("TU-63 — preferência recusada e reenviada repete a recusa com o mesmo código")
    void givenPreferenceOperationAlreadyRejected_whenApplying_thenReplaysRejection() {
        SyncMutationOperation operation = operation(SyncOperationKind.CARD_SUSPENSION, List.of());
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(operation));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(operation));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(operation)).thenReturn(false);
        br.com.certamecards.sync.domain.MutationReceipt receipt = new br.com.certamecards.sync.domain.MutationReceipt(
                USER_ID,
                operation.operationId(),
                "hash",
                "card_suspension",
                order(),
                "action_required",
                null,
                null,
                null,
                "not_applicable",
                NOW);
        when(receipts.find(USER_ID, operation.operationId())).thenReturn(Optional.of(receipt));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        assertThat(response.results().get(0).outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        assertThat(response.results().get(0).error().code()).isEqualTo("not_applicable");
    }

    @Test
    void givenPreferenceOperationAlreadyApplied_whenApplying_thenReturnsDuplicate() {
        SyncMutationOperation operation = operation(SyncOperationKind.DECK_RESET, List.of());
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(operation));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(operation));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(operation)).thenReturn(false);
        when(handlers.forOperation(operation))
                .thenReturn((userId, context) -> new MutationResult(
                        context.operation().operationId(),
                        MutationOutcome.APPLIED,
                        null,
                        1L,
                        context.order(),
                        null,
                        null));
        br.com.certamecards.sync.domain.MutationReceipt receipt = new br.com.certamecards.sync.domain.MutationReceipt(
                USER_ID, operation.operationId(), "hash", "deck_reset", order(), "applied", null, 1L, null, null, NOW);
        when(receipts.find(USER_ID, operation.operationId())).thenReturn(Optional.of(receipt));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        assertThat(response.results().get(0).outcome()).isEqualTo(MutationOutcome.DUPLICATE);
    }

    @Test
    void givenEntityOperation_whenApplying_thenDelegatesToCoordinator() {
        SyncMutationOperation operation = operation(SyncOperationKind.DECK_CREATE, List.of());
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(operation));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(operation));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(operation)).thenReturn(true);
        when(handlers.forOperation(operation))
                .thenReturn((userId, context) -> new MutationResult(
                        context.operation().operationId(),
                        MutationOutcome.APPLIED,
                        1,
                        2L,
                        context.order(),
                        null,
                        null));
        when(coordinator.execute(any()))
                .thenReturn(new MutationResult(
                        operation.operationId(), MutationOutcome.APPLIED, 1, 2L, order(), null, null));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        assertThat(response.results().get(0).outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(coordinator).execute(any());
    }

    @Test
    void givenDependencyMissingFromResults_whenApplying_thenChildIsNotBlocked() {
        SyncMutationOperation child = operation(SyncOperationKind.CARD_CREATE, List.of(UUID.randomUUID()));
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(child));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(child));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(child)).thenReturn(true);
        when(coordinator.execute(any()))
                .thenReturn(
                        new MutationResult(child.operationId(), MutationOutcome.APPLIED, 1, 2L, order(), null, null));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        assertThat(response.results().get(0).outcome()).isEqualTo(MutationOutcome.APPLIED);
    }

    @Test
    void givenDependencyApplied_whenApplying_thenChildIsNotBlocked() {
        SyncMutationOperation parent = operation(SyncOperationKind.DECK_CREATE, List.of());
        SyncMutationOperation child = operation(SyncOperationKind.CARD_CREATE, List.of(parent.operationId()));
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(parent, child));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(parent, child));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(parent)).thenReturn(true);
        when(handlers.needsEntityHead(child)).thenReturn(true);
        when(coordinator.execute(any()))
                .thenReturn(new MutationResult(UUID.randomUUID(), MutationOutcome.APPLIED, 1, 2L, order(), null, null));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        assertThat(response.results())
                .extracting(MutationResult::outcome)
                .doesNotContain(MutationOutcome.DEPENDENCY_BLOCKED);
    }

    @Test
    void givenDependencyDuplicate_whenApplying_thenChildIsNotBlocked() {
        SyncMutationOperation parent = operation(SyncOperationKind.DECK_CREATE, List.of());
        SyncMutationOperation child = operation(SyncOperationKind.CARD_CREATE, List.of(parent.operationId()));
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(parent, child));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(parent, child));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(parent)).thenReturn(true);
        when(handlers.needsEntityHead(child)).thenReturn(true);
        when(coordinator.execute(any()))
                .thenReturn(
                        new MutationResult(UUID.randomUUID(), MutationOutcome.DUPLICATE, 1, 2L, order(), null, null))
                .thenReturn(new MutationResult(UUID.randomUUID(), MutationOutcome.APPLIED, 1, 2L, order(), null, null));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        assertThat(response.results())
                .extracting(MutationResult::outcome)
                .doesNotContain(MutationOutcome.DEPENDENCY_BLOCKED);
    }

    @Test
    void givenPreferenceHandlerReturnsError_whenApplying_thenDelegatesToPreferenceWriter() {
        SyncMutationOperation operation = operation(SyncOperationKind.DECK_RESET, List.of());
        SyncMutationBatch batch = new SyncMutationBatch(DEVICE_ID, List.of(operation));
        when(dependencyResolver.order(batch.operations())).thenReturn(List.of(operation));
        when(orderNormalizer.normalize(any(), any(), any(), any(), any())).thenReturn(order());
        when(handlers.needsEntityHead(operation)).thenReturn(false);
        when(handlers.forOperation(operation))
                .thenReturn((userId, context) -> new MutationResult(
                        context.operation().operationId(),
                        MutationOutcome.ACTION_REQUIRED,
                        null,
                        null,
                        context.order(),
                        null,
                        new br.com.certamecards.sync.domain.MutationError("not_applicable", "erro")));
        when(receipts.find(USER_ID, operation.operationId())).thenReturn(Optional.empty());
        when(preferenceCoordinator.attempt(any()))
                .thenReturn(new MutationResult(
                        operation.operationId(),
                        MutationOutcome.ACTION_REQUIRED,
                        null,
                        null,
                        order(),
                        null,
                        new br.com.certamecards.sync.domain.MutationError("not_applicable", "erro")));

        SyncMutationResponse response = service.apply(USER_ID, batch);

        assertThat(response.results().get(0).outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        verify(preferenceCoordinator).attempt(any());
    }

    private SyncMutationOperation operation(SyncOperationKind kind, List<UUID> dependsOn) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                kind,
                UUID.randomUUID(),
                null,
                null,
                null,
                dependsOn,
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }

    private EventOrder order() {
        return new EventOrder(NOW, 0, DEVICE_ID, UUID.randomUUID());
    }
}
