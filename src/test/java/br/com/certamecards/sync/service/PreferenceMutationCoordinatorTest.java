package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
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

class PreferenceMutationCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private static final UUID USER_ID = UUID.randomUUID();
    private final PreferenceMutationWriter writer = mock(PreferenceMutationWriter.class);
    private final PreferenceMutationCoordinator coordinator = new PreferenceMutationCoordinator(writer);

    @Test
    void givenSuccessfulAttempt_whenCoordinating_thenReturnsWriterResult() {
        EntityMutationCommand command = command();
        MutationResult applied = new MutationResult(
                command.operation().operationId(), MutationOutcome.APPLIED, null, 1L, command.order(), null, null);
        when(writer.attempt(command)).thenReturn(applied);

        MutationResult result = coordinator.attempt(command);

        assertThat(result).isEqualTo(applied);
    }

    @Test
    void givenNotFoundFromWriter_whenCoordinating_thenRecordsRejectionAsNotApplicable() {
        EntityMutationCommand command = command();
        when(writer.attempt(command)).thenThrow(ApiException.of(ErrorCode.NOT_FOUND));
        when(writer.recordRejection(command, "not_applicable"))
                .thenReturn(new MutationResult(
                        command.operation().operationId(),
                        MutationOutcome.ACTION_REQUIRED,
                        null,
                        null,
                        command.order(),
                        null,
                        new br.com.certamecards.sync.domain.MutationError("not_applicable", "erro")));

        MutationResult result = coordinator.attempt(command);

        assertThat(result.error().code()).isEqualTo("not_applicable");
        verify(writer).recordRejection(command, "not_applicable");
    }

    @Test
    void givenOtherApiExceptionFromWriter_whenCoordinating_thenRecordsRejectionWithSameCode() {
        EntityMutationCommand command = command();
        when(writer.attempt(command)).thenThrow(ApiException.of(ErrorCode.FORBIDDEN));

        coordinator.attempt(command);

        verify(writer).recordRejection(command, "forbidden");
    }

    @Test
    void givenIllegalArgument_whenCoordinating_thenRecordsRejectionAsValidationFailed() {
        EntityMutationCommand command = command();
        when(writer.attempt(command)).thenThrow(new IllegalArgumentException("bad payload"));

        coordinator.attempt(command);

        verify(writer).recordRejection(command, "validation_failed");
    }

    private EntityMutationCommand command() {
        SyncMutationOperation operation = new SyncMutationOperation(
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
        EventOrder order = new EventOrder(NOW, 0, UUID.randomUUID(), UUID.randomUUID());
        return new EntityMutationCommand(USER_ID, operation, order, (userId, context) -> null, "hash");
    }
}
