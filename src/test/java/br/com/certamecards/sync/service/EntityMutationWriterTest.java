package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationReceipt;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncEntityHead;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import br.com.certamecards.sync.persistence.SyncConflictWriter;
import br.com.certamecards.sync.persistence.SyncEntityHeadRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class EntityMutationWriterTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DEVICE_ID = UUID.randomUUID();
    private final SyncEntityHeadRepository heads = mock(SyncEntityHeadRepository.class);
    private final SyncConflictWriter conflicts = mock(SyncConflictWriter.class);
    private final EntityConflictRegistrar conflictRegistrar = new EntityConflictRegistrar(conflicts);
    private final MutationReceiptService receipts = mock(MutationReceiptService.class);
    private final EntityMutationWriter writer = new EntityMutationWriter(heads, conflictRegistrar, receipts);

    @Test
    void givenNewDeck_whenAttempting_thenAppliesAndPersistsHeadAndReceipt() {
        SyncMutationOperation operation = operation(UUID.randomUUID());
        EventOrder order = order(operation.operationId(), 1);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(head(operation, null, false));

        MutationResult result = writer.attempt(command(operation, order));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(heads).save(any(SyncEntityHead.class));
        verify(receipts).reserve(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenOlderConcurrentEdit_whenAttempting_thenPreservesItAsConflict() {
        UUID winner = UUID.randomUUID();
        SyncMutationOperation operation = operation(UUID.randomUUID());
        EventOrder order = order(operation.operationId(), 1);
        SyncEntityHead existing = head(operation, new EventOrder(NOW, 2, DEVICE_ID, winner), false);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(existing);
        when(conflicts.write(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());

        MutationResult result = writer.attempt(command(operation, order));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.CONFLICT);
        verify(conflicts).write(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenCausalSuccessor_whenAttempting_thenAppliesDespiteOlderOrder() {
        UUID predecessor = UUID.randomUUID();
        SyncMutationOperation operation = operationWithPredecessor(UUID.randomUUID(), predecessor, null);
        EventOrder olderOrder = order(operation.operationId(), 1);
        SyncEntityHead existing = head(operation, new EventOrder(NOW, 5, DEVICE_ID, predecessor), false);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(existing);

        MutationResult result = writer.attempt(command(operation, olderOrder));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(heads).save(any(SyncEntityHead.class));
    }

    @Test
    void givenBaseVersionMatches_whenAttempting_thenApplies() {
        SyncMutationOperation operation = operationWithBaseVersion(UUID.randomUUID(), 3);
        EventOrder olderOrder = order(operation.operationId(), 1);
        SyncEntityHead existing = new SyncEntityHead(
                USER_ID,
                "deck",
                operation.entityId(),
                null,
                3,
                UUID.randomUUID(),
                new EventOrder(NOW, 9, DEVICE_ID, UUID.randomUUID()),
                false);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(existing);

        MutationResult result = writer.attempt(command(operation, olderOrder));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
    }

    @Test
    void givenDeleteWithOlderOrder_whenAttempting_thenDeleteStillWinsAndPreservesEditAsConflict() {
        SyncMutationOperation operation = operation(UUID.randomUUID(), SyncOperationKind.DECK_DELETE);
        EventOrder olderOrder = order(operation.operationId(), 1);
        SyncEntityHead existing = head(operation, new EventOrder(NOW, 9, DEVICE_ID, UUID.randomUUID()), false);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(existing);

        MutationResult result = writer.attempt(command(operation, olderOrder));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(heads).deleteChildren(any(), any(), any());
        verify(conflicts)
                .write(
                        eq(USER_ID),
                        eq("deck"),
                        eq(operation.entityId()),
                        any(),
                        eq(existing.winningOperationId()),
                        eq(operation.operationId()),
                        eq(br.com.certamecards.sync.domain.ConflictReason.DELETE_WINS),
                        any(),
                        any());
    }

    @Test
    void givenNewerNonCausalEdit_whenAttempting_thenAppliesAndPreservesPreviousEditAsConflict() {
        SyncMutationOperation operation = operation(UUID.randomUUID());
        EventOrder newerOrder = order(operation.operationId(), 9);
        UUID previousWinner = UUID.randomUUID();
        SyncEntityHead existing = head(operation, new EventOrder(NOW, 1, DEVICE_ID, previousWinner), false);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(existing);

        MutationResult result = writer.attempt(command(operation, newerOrder));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(conflicts)
                .write(
                        eq(USER_ID),
                        eq("deck"),
                        eq(operation.entityId()),
                        any(),
                        eq(previousWinner),
                        eq(operation.operationId()),
                        eq(br.com.certamecards.sync.domain.ConflictReason.CONCURRENT_EDIT),
                        any(),
                        any());
    }

    @Test
    @DisplayName("TU-58 — edição que chega depois da exclusão vira conflito recuperável, sem reaplicar")
    void givenDeletedEntityAndEditArrivingLater_whenAttempting_thenPreservesEditAsDeleteWinsConflict() {
        SyncMutationOperation operation = operation(UUID.randomUUID(), SyncOperationKind.DECK_UPDATE);
        EventOrder order = order(operation.operationId(), 1);
        UUID winner = UUID.randomUUID();
        UUID conflictId = UUID.randomUUID();
        SyncEntityHead existing = head(operation, new EventOrder(NOW, 9, DEVICE_ID, winner), true);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(existing);
        when(conflicts.write(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(conflictId);

        MutationResult result = writer.attempt(command(operation, order));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.CONFLICT);
        assertThat(result.conflictId()).isEqualTo(conflictId);
        verify(conflicts)
                .write(
                        eq(USER_ID),
                        eq("deck"),
                        eq(operation.entityId()),
                        any(),
                        eq(operation.operationId()),
                        eq(winner),
                        eq(br.com.certamecards.sync.domain.ConflictReason.DELETE_WINS),
                        any(),
                        any());
        verify(heads, never()).save(any());
    }

    @Test
    void givenDeletedEntityAndSecondDelete_whenAttempting_thenAppliesWithoutChangesOrConflict() {
        SyncMutationOperation operation = operation(UUID.randomUUID(), SyncOperationKind.DECK_DELETE);
        EventOrder order = order(operation.operationId(), 1);
        SyncEntityHead existing = head(operation, new EventOrder(NOW, 9, DEVICE_ID, UUID.randomUUID()), true);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(existing);

        MutationResult result = writer.attempt(command(operation, order));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.error()).isNull();
        verify(heads, never()).save(any());
        verify(conflicts, never()).write(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenTombstoneAndRestoreOperation_whenAttempting_thenAllowsRestore() {
        UUID deleteOperationId = UUID.randomUUID();
        SyncMutationOperation operation = restoreOperation(UUID.randomUUID(), deleteOperationId);
        EventOrder order = order(operation.operationId(), 1);
        SyncEntityHead existing = new SyncEntityHead(
                USER_ID,
                "card",
                operation.entityId(),
                null,
                0,
                deleteOperationId,
                new EventOrder(NOW, 9, DEVICE_ID, deleteOperationId),
                true);
        when(heads.lock(USER_ID, "card", operation.entityId(), null)).thenReturn(existing);

        MutationResult result = writer.attempt(command(operation, order));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(heads).save(any(SyncEntityHead.class));
    }

    @Test
    void givenParentDeckDeleted_whenAttemptingCard_thenRejectsAsParentDeleted() {
        UUID parentId = UUID.randomUUID();
        SyncMutationOperation operation = cardOperation(UUID.randomUUID(), parentId);
        EventOrder order = order(operation.operationId(), 1);
        SyncEntityHead deletedParent = new SyncEntityHead(
                USER_ID,
                "deck",
                parentId,
                null,
                1,
                UUID.randomUUID(),
                new EventOrder(NOW, 1, DEVICE_ID, UUID.randomUUID()),
                true);
        when(heads.lock(USER_ID, "deck", parentId, null)).thenReturn(deletedParent);
        when(conflicts.write(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());

        MutationResult result = writer.attempt(command(operation, order));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        assertThat(result.error().code()).isEqualTo("parent_deleted");
    }

    @Test
    void givenExistingReceipt_whenBuildingDuplicate_thenReturnsDuplicateOutcome() {
        SyncMutationOperation operation = operation(UUID.randomUUID());
        EventOrder order = order(operation.operationId(), 1);
        MutationReceipt receipt = new MutationReceipt(
                USER_ID, operation.operationId(), "hash", "deck_create", order, "applied", 1, 2L, null, null, NOW);

        MutationResult result = writer.duplicate(receipt, "hash");

        assertThat(result.outcome()).isEqualTo(MutationOutcome.DUPLICATE);
    }

    @Test
    @DisplayName(
            "TU-63 — recibo de recusa repete a recusa, nunca vira duplicate (evita dar a operação por sincronizada)")
    void givenExistingReceiptWithErrorCode_whenBuildingDuplicate_thenIncludesError() {
        SyncMutationOperation operation = operation(UUID.randomUUID());
        EventOrder order = order(operation.operationId(), 1);
        MutationReceipt receipt = new MutationReceipt(
                USER_ID,
                operation.operationId(),
                "hash",
                "deck_create",
                order,
                "action_required",
                null,
                null,
                null,
                "validation_failed",
                NOW);

        MutationResult result = writer.duplicate(receipt, "hash");

        assertThat(result.outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        assertThat(result.error().code()).isEqualTo("validation_failed");
    }

    @Test
    void givenParentIdOnNonCardOperation_whenAttempting_thenDoesNotLockParent() {
        SyncMutationOperation operation = operationWithParentId(UUID.randomUUID(), UUID.randomUUID());
        EventOrder order = order(operation.operationId(), 1);
        when(heads.lock(USER_ID, "deck", operation.entityId(), operation.parentId()))
                .thenReturn(head(operation, null, false));

        MutationResult result = writer.attempt(command(operation, order));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        verify(heads, never()).lock(eq(USER_ID), eq("deck"), eq(operation.parentId()), any());
    }

    @Test
    void givenTombstoneAndNonCausalRestore_whenAttempting_thenPreservesAsDeleteWinsConflict() {
        UUID deleteOperationId = UUID.randomUUID();
        SyncMutationOperation operation = restoreOperation(UUID.randomUUID(), UUID.randomUUID());
        EventOrder olderOrder = order(operation.operationId(), 1);
        SyncEntityHead existing = new SyncEntityHead(
                USER_ID,
                "card",
                operation.entityId(),
                null,
                0,
                deleteOperationId,
                new EventOrder(NOW, 9, DEVICE_ID, deleteOperationId),
                true);
        when(heads.lock(USER_ID, "card", operation.entityId(), null)).thenReturn(existing);
        when(conflicts.write(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(br.com.certamecards.sync.domain.ConflictReason.DELETE_WINS),
                        any(),
                        any()))
                .thenReturn(UUID.randomUUID());

        MutationResult result = writer.attempt(command(operation, olderOrder));

        assertThat(result.outcome()).isEqualTo(MutationOutcome.CONFLICT);
        verify(conflicts)
                .write(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(br.com.certamecards.sync.domain.ConflictReason.DELETE_WINS),
                        any(),
                        any());
    }

    @Test
    void givenHandlerThrowsApiException_whenAttempting_thenPropagatesForOuterRollback() {
        SyncMutationOperation operation = operation(UUID.randomUUID());
        EventOrder order = order(operation.operationId(), 1);
        when(heads.lock(USER_ID, "deck", operation.entityId(), null)).thenReturn(head(operation, null, false));
        EntityMutationCommand command = new EntityMutationCommand(
                USER_ID,
                operation,
                order,
                (userId, context) -> {
                    throw ApiException.of(ErrorCode.VALIDATION_FAILED);
                },
                "hash");

        assertThatThrownBy(() -> writer.attempt(command)).isInstanceOf(ApiException.class);
    }

    @Test
    void givenRejection_whenRecording_thenReservesReceiptWithCode() {
        SyncMutationOperation operation = operation(UUID.randomUUID());
        EventOrder order = order(operation.operationId(), 1);

        MutationResult result = writer.recordRejection(command(operation, order), "validation_failed");

        assertThat(result.outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        assertThat(result.error().code()).isEqualTo("validation_failed");
        verify(receipts).reserve(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private EntityMutationCommand command(SyncMutationOperation operation, EventOrder order) {
        return new EntityMutationCommand(
                USER_ID,
                operation,
                order,
                (userId, context) -> new MutationResult(
                        context.operation().operationId(), MutationOutcome.APPLIED, 1, 2L, context.order(), null, null),
                "hash");
    }

    private SyncEntityHead head(SyncMutationOperation operation, EventOrder order, boolean deleted) {
        return new SyncEntityHead(
                USER_ID,
                "deck",
                operation.entityId(),
                null,
                0,
                order == null ? null : order.operationId(),
                order,
                deleted);
    }

    private SyncMutationOperation operation(UUID operationId) {
        return new SyncMutationOperation(
                operationId,
                SyncOperationKind.DECK_CREATE,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }

    private EventOrder order(UUID operationId, int counter) {
        return new EventOrder(NOW, counter, DEVICE_ID, operationId);
    }

    private SyncMutationOperation operation(UUID operationId, SyncOperationKind kind) {
        return new SyncMutationOperation(
                operationId,
                kind,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }

    private SyncMutationOperation operationWithPredecessor(
            UUID operationId, UUID predecessorOperationId, Integer baseVersion) {
        return new SyncMutationOperation(
                operationId,
                SyncOperationKind.DECK_UPDATE,
                UUID.randomUUID(),
                null,
                baseVersion,
                predecessorOperationId,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }

    private SyncMutationOperation operationWithBaseVersion(UUID operationId, int baseVersion) {
        return operationWithPredecessor(operationId, null, baseVersion);
    }

    private SyncMutationOperation restoreOperation(UUID operationId, UUID predecessorOperationId) {
        return new SyncMutationOperation(
                operationId,
                SyncOperationKind.CONFLICT_RESTORE,
                UUID.randomUUID(),
                null,
                null,
                predecessorOperationId,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }

    private SyncMutationOperation operationWithParentId(UUID operationId, UUID parentId) {
        return new SyncMutationOperation(
                operationId,
                SyncOperationKind.DECK_UPDATE,
                UUID.randomUUID(),
                parentId,
                0,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }

    private SyncMutationOperation cardOperation(UUID operationId, UUID parentId) {
        return new SyncMutationOperation(
                operationId,
                SyncOperationKind.CARD_CREATE,
                UUID.randomUUID(),
                parentId,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }
}
