package br.com.certamecards.sync.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.ConflictReason;
import br.com.certamecards.sync.domain.SyncEntityHead;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import br.com.certamecards.sync.persistence.SyncConflictWriter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class EntityConflictRegistrarTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DEVICE_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    private final SyncConflictWriter conflicts = mock(SyncConflictWriter.class);
    private final SyncMutationHandler handler = mock(SyncMutationHandler.class);
    private final EntityConflictRegistrar registrar = new EntityConflictRegistrar(conflicts);

    @Test
    @DisplayName("CA-20 — restauração que perde guarda o conteúdo restaurado, não o payload bruto da operação")
    void givenLosingRestore_whenRegistering_thenStoresRestoredContentAsLosingSnapshot() {
        ObjectNode snapshot =
                JsonNodeFactory.instance.objectNode().put("front", "Versão A").put("back", "Verso");
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("conflictId", UUID.randomUUID().toString());
        payload.set("snapshot", snapshot);
        SyncMutationOperation operation = operation(SyncOperationKind.CONFLICT_RESTORE, payload);
        SyncEntityHead head = head(operation.entityId());
        when(handler.currentSnapshot(USER_ID, operation.entityId())).thenReturn("{\"front\":\"Versão B\"}");

        registrar.registerLosingIncoming(
                new EntityMutationCommand(USER_ID, operation, order(operation), handler, "h"), head);

        verify(conflicts)
                .write(
                        eq(USER_ID),
                        eq("card"),
                        eq(operation.entityId()),
                        any(),
                        eq(operation.operationId()),
                        eq(head.winningOperationId()),
                        eq(ConflictReason.CONCURRENT_EDIT),
                        eq(snapshot.toString()),
                        eq("{\"front\":\"Versão B\"}"));
    }

    @Test
    @DisplayName("CA-16 — edição comum que perde guarda o próprio payload como versão guardada")
    void givenLosingEdit_whenRegistering_thenStoresPayloadAsLosingSnapshot() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode().put("front", "Editado");
        SyncMutationOperation operation = operation(SyncOperationKind.CARD_UPDATE, payload);
        SyncEntityHead head = head(operation.entityId());

        registrar.registerLosingIncoming(
                new EntityMutationCommand(USER_ID, operation, order(operation), handler, "h"), head);

        verify(conflicts).write(any(), any(), any(), any(), any(), any(), any(), eq(payload.toString()), any());
    }

    private SyncMutationOperation operation(SyncOperationKind kind, ObjectNode payload) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                kind,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                payload);
    }

    private EventOrder order(SyncMutationOperation operation) {
        return new EventOrder(NOW, 0, DEVICE_ID, operation.operationId());
    }

    private SyncEntityHead head(UUID entityId) {
        UUID winner = UUID.randomUUID();
        return new SyncEntityHead(
                USER_ID, "card", entityId, null, 2, winner, new EventOrder(NOW, 9, DEVICE_ID, winner), false);
    }
}
