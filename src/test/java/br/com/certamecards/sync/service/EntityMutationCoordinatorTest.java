package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class EntityMutationCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final MutationReceiptService receipts = mock(MutationReceiptService.class);
    private final EntityMutationWriter writer = mock(EntityMutationWriter.class);
    private final EntityMutationCoordinator coordinator = new EntityMutationCoordinator(receipts, writer);

    @Test
    void givenExistingReceipt_whenExecuting_thenReturnsDuplicateWithoutAttempting() {
        SyncMutationOperation operation = operation();
        EntityMutationCommand command = command(operation);
        MutationReceipt receipt = new MutationReceipt(
                USER_ID, operation.operationId(), "hash", "deck_create", order(), "applied", 1, 2L, null, null, NOW);
        when(receipts.find(USER_ID, operation.operationId())).thenReturn(Optional.of(receipt));
        MutationResult duplicate = result(MutationOutcome.DUPLICATE);
        when(writer.duplicate(receipt, "hash")).thenReturn(duplicate);

        MutationResult result = coordinator.execute(command);

        assertThat(result).isSameAs(duplicate);
        verify(writer, never()).attempt(any());
    }

    @Test
    void givenNoExistingReceipt_whenExecuting_thenDelegatesToWriterAttempt() {
        SyncMutationOperation operation = operation();
        EntityMutationCommand command = command(operation);
        when(receipts.find(USER_ID, operation.operationId())).thenReturn(Optional.empty());
        MutationResult applied = result(MutationOutcome.APPLIED);
        when(writer.attempt(command)).thenReturn(applied);

        MutationResult result = coordinator.execute(command);

        assertThat(result).isSameAs(applied);
    }

    @Test
    void givenWriterThrowsApiException_whenExecuting_thenRecordsRejectionWithErrorCode() {
        SyncMutationOperation operation = operation();
        EntityMutationCommand command = command(operation);
        when(receipts.find(USER_ID, operation.operationId())).thenReturn(Optional.empty());
        when(writer.attempt(command)).thenThrow(ApiException.of(ErrorCode.VERSION_CONFLICT));
        MutationResult rejected = result(MutationOutcome.ACTION_REQUIRED);
        when(writer.recordRejection(command, ErrorCode.VERSION_CONFLICT.code())).thenReturn(rejected);

        MutationResult result = coordinator.execute(command);

        assertThat(result).isSameAs(rejected);
        verify(writer).recordRejection(command, "version_conflict");
    }

    @Test
    void givenWriterThrowsIllegalArgument_whenExecuting_thenRecordsRejectionAsValidationFailed() {
        SyncMutationOperation operation = operation();
        EntityMutationCommand command = command(operation);
        when(receipts.find(USER_ID, operation.operationId())).thenReturn(Optional.empty());
        when(writer.attempt(command)).thenThrow(new IllegalArgumentException("baseVersion is required"));
        MutationResult rejected = result(MutationOutcome.ACTION_REQUIRED);
        when(writer.recordRejection(eq(command), eq("validation_failed"))).thenReturn(rejected);

        MutationResult result = coordinator.execute(command);

        assertThat(result).isSameAs(rejected);
        verify(writer).recordRejection(command, "validation_failed");
    }

    private EntityMutationCommand command(SyncMutationOperation operation) {
        return new EntityMutationCommand(
                USER_ID, operation, order(), (userId, context) -> result(MutationOutcome.APPLIED), "hash");
    }

    private MutationResult result(MutationOutcome outcome) {
        return new MutationResult(UUID.randomUUID(), outcome, null, null, order(), null, null);
    }

    private SyncMutationOperation operation() {
        return new SyncMutationOperation(
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
                JsonNodeFactory.instance.objectNode());
    }

    private EventOrder order() {
        return new EventOrder(NOW, 0, UUID.randomUUID(), UUID.randomUUID());
    }
}
