package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.ConflictReason;
import br.com.certamecards.sync.domain.SyncEntityHead;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import br.com.certamecards.sync.persistence.SyncConflictWriter;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EntityConflictRegistrar {

    private static final String RESTORE_SNAPSHOT_FIELD = "snapshot";

    private final SyncConflictWriter conflicts;

    public EntityConflictRegistrar(SyncConflictWriter conflicts) {
        this.conflicts = conflicts;
    }

    public boolean isConcurrentSupersede(SyncEntityHead head, SyncMutationOperation operation) {
        return head.winningOperationId() != null
                && !head.isCausalSuccessor(operation)
                && !head.hasBaseVersion(operation);
    }

    public void registerSupersededEdit(EntityMutationCommand command, SyncEntityHead head) {
        String previousSnapshot = command.handler().currentSnapshot(command.userId(), head.entityId());
        ConflictReason reason =
                isDelete(command.operation()) ? ConflictReason.DELETE_WINS : ConflictReason.CONCURRENT_EDIT;
        conflicts.write(
                command.userId(),
                head.entityType(),
                head.entityId(),
                head.parentId(),
                head.winningOperationId(),
                command.operation().operationId(),
                reason,
                previousSnapshot,
                snapshot(command.operation()));
    }

    public UUID registerLosingIncoming(EntityMutationCommand command, SyncEntityHead head) {
        ConflictReason reason = isDelete(command.operation()) || head.deleted()
                ? ConflictReason.DELETE_WINS
                : ConflictReason.CONCURRENT_EDIT;
        String winningSnapshot = command.handler().currentSnapshot(command.userId(), head.entityId());
        return conflicts.write(
                command.userId(),
                head.entityType(),
                command.operation().entityId(),
                command.operation().parentId(),
                command.operation().operationId(),
                head.winningOperationId(),
                reason,
                snapshot(command.operation()),
                winningSnapshot);
    }

    public UUID registerParentDeleted(EntityMutationCommand command, SyncEntityHead parent) {
        return conflicts.write(
                command.userId(),
                "card",
                command.operation().entityId(),
                command.operation().parentId(),
                command.operation().operationId(),
                parent.winningOperationId(),
                ConflictReason.PARENT_DELETED,
                snapshot(command.operation()),
                "{}");
    }

    private String snapshot(SyncMutationOperation operation) {
        if (operation.kind() == SyncOperationKind.CONFLICT_RESTORE
                && operation.payload().has(RESTORE_SNAPSHOT_FIELD)) {
            return operation.payload().get(RESTORE_SNAPSHOT_FIELD).toString();
        }
        return operation.payload().toString();
    }

    private boolean isDelete(SyncMutationOperation operation) {
        return operation.kind().value().endsWith("_delete");
    }
}
