package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class PreferenceMutationWriterTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final MutationReceiptService receipts = mock(MutationReceiptService.class);
    private final PreferenceMutationWriter writer = new PreferenceMutationWriter(receipts);

    @Test
    void givenSuccessfulHandler_whenAttempting_thenReservesReceiptWithinSameCall() {
        SyncMutationOperation operation = operation();
        EventOrder order = order();
        SyncMutationHandler handler = (userId, context) -> new MutationResult(
                context.operation().operationId(), MutationOutcome.APPLIED, null, 5L, context.order(), null, null);
        EntityMutationCommand command = new EntityMutationCommand(USER_ID, operation, order, handler, "hash");

        MutationResult result = writer.attempt(command);

        assertThat(result.outcome()).isEqualTo(MutationOutcome.APPLIED);
        assertThat(result.changeSeq()).isEqualTo(5L);
        verify(receipts)
                .reserve(
                        eq(USER_ID),
                        eq(operation.operationId()),
                        eq("hash"),
                        eq(operation.kind().value()),
                        eq(order),
                        eq("applied"),
                        any(),
                        eq(5L),
                        any(),
                        any(),
                        eq(order.eventAt()));
    }

    @Test
    void givenHandlerReturningError_whenAttempting_thenReservesReceiptWithErrorCode() {
        SyncMutationOperation operation = operation();
        EventOrder order = order();
        SyncMutationHandler handler = (userId, context) -> new MutationResult(
                context.operation().operationId(),
                MutationOutcome.ACTION_REQUIRED,
                null,
                null,
                context.order(),
                null,
                new br.com.certamecards.sync.domain.MutationError("not_applicable", "erro"));
        EntityMutationCommand command = new EntityMutationCommand(USER_ID, operation, order, handler, "hash");

        MutationResult result = writer.attempt(command);

        assertThat(result.error().code()).isEqualTo("not_applicable");
        verify(receipts)
                .reserve(
                        eq(USER_ID),
                        eq(operation.operationId()),
                        eq("hash"),
                        eq(operation.kind().value()),
                        eq(order),
                        eq("action_required"),
                        any(),
                        any(),
                        any(),
                        eq("not_applicable"),
                        eq(order.eventAt()));
    }

    @Test
    void givenRejectionCode_whenRecording_thenReservesActionRequiredReceipt() {
        SyncMutationOperation operation = operation();
        EventOrder order = order();
        EntityMutationCommand command =
                new EntityMutationCommand(USER_ID, operation, order, (userId, context) -> null, "hash");

        MutationResult result = writer.recordRejection(command, "not_applicable");

        assertThat(result.outcome()).isEqualTo(MutationOutcome.ACTION_REQUIRED);
        assertThat(result.error().code()).isEqualTo("not_applicable");
        verify(receipts)
                .reserve(
                        eq(USER_ID),
                        eq(operation.operationId()),
                        eq("hash"),
                        eq(operation.kind().value()),
                        eq(order),
                        eq("action_required"),
                        any(),
                        any(),
                        any(),
                        eq("not_applicable"),
                        eq(order.eventAt()));
    }

    private SyncMutationOperation operation() {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                SyncOperationKind.DECK_RESET,
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
