package br.com.certamecards.sync.service;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationError;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationReceipt;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncEntityHead;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.persistence.SyncEntityHeadRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EntityMutationWriter {

    private final SyncEntityHeadRepository heads;
    private final EntityConflictRegistrar conflictRegistrar;
    private final MutationReceiptService receipts;

    public EntityMutationWriter(
            SyncEntityHeadRepository heads,
            EntityConflictRegistrar conflictRegistrar,
            MutationReceiptService receipts) {
        this.heads = heads;
        this.conflictRegistrar = conflictRegistrar;
        this.receipts = receipts;
    }

    @Transactional
    public MutationResult attempt(EntityMutationCommand command) {
        SyncMutationOperation operation = command.operation();
        SyncEntityHead parent = lockParent(command);
        MutationResult result = parent != null && parent.deleted()
                ? parentDeleted(command, parent)
                : applyOrResolve(
                        command,
                        heads.lock(command.userId(), typeOf(operation), operation.entityId(), operation.parentId()));
        return reserve(command, result);
    }

    @Transactional
    public MutationResult recordRejection(EntityMutationCommand command, String code) {
        return reserve(command, rejected(command, code));
    }

    public MutationResult duplicate(MutationReceipt receipt, String requestHash) {
        receipts.ensureSameRequest(receipt, requestHash);
        MutationError error = receipt.errorCode() == null
                ? null
                : new MutationError(receipt.errorCode(), "A alteração foi recusada.");
        return new MutationResult(
                receipt.operationId(),
                receipt.replayOutcome(),
                receipt.entityVersion(),
                receipt.changeSeq(),
                receipt.order(),
                receipt.conflictId(),
                error);
    }

    private MutationResult applyOrResolve(EntityMutationCommand command, SyncEntityHead head) {
        SyncMutationOperation operation = command.operation();
        if (head.deleted() && !isRestore(operation)) return deleted(command, head);
        if (canApply(head, operation, command.order())) return apply(command, head);
        return resolveConcurrent(command, head);
    }

    private MutationResult reserve(EntityMutationCommand command, MutationResult result) {
        receipts.reserve(
                command.userId(),
                command.operation().operationId(),
                command.requestHash(),
                command.operation().kind().value(),
                command.order(),
                result.outcome().value(),
                result.entityVersion(),
                result.changeSeq(),
                result.conflictId(),
                result.error() == null ? null : result.error().code(),
                command.order().eventAt());
        return result;
    }

    private SyncEntityHead lockParent(EntityMutationCommand command) {
        SyncMutationOperation operation = command.operation();
        if (operation.parentId() == null || !typeOf(operation).equals("card")) return null;
        return heads.lock(command.userId(), "deck", operation.parentId(), null);
    }

    private boolean canApply(SyncEntityHead head, SyncMutationOperation operation, EventOrder order) {
        if (head.winningOperationId() == null) return true;
        if (head.isCausalSuccessor(operation) || head.hasBaseVersion(operation)) return true;
        return isDelete(operation) && !head.deleted()
                || !isDelete(operation) && !head.deleted() && EventOrder.CANONICAL.compare(order, head.order()) > 0;
    }

    private MutationResult apply(EntityMutationCommand command, SyncEntityHead head) {
        if (!isRestore(command.operation()) && conflictRegistrar.isConcurrentSupersede(head, command.operation())) {
            conflictRegistrar.registerSupersededEdit(command, head);
        }
        MutationResult result = command.handler()
                .handle(command.userId(), new MutationContext(command.operation(), command.order(), head.version()));
        heads.save(updated(head, command.operation(), command.order(), result.entityVersion()));
        if (isDeckDelete(command.operation())) {
            heads.deleteChildren(command.userId(), command.operation().entityId(), command.order());
        }
        return result;
    }

    private SyncEntityHead updated(
            SyncEntityHead head, SyncMutationOperation operation, EventOrder order, Integer version) {
        int nextVersion = version == null ? head.version() + 1 : version;
        return new SyncEntityHead(
                head.userId(),
                head.entityType(),
                head.entityId(),
                operation.parentId(),
                nextVersion,
                operation.operationId(),
                order,
                isDelete(operation));
    }

    private MutationResult resolveConcurrent(EntityMutationCommand command, SyncEntityHead head) {
        UUID conflictId = conflictRegistrar.registerLosingIncoming(command, head);
        return new MutationResult(
                command.operation().operationId(),
                MutationOutcome.CONFLICT,
                head.version(),
                null,
                command.order(),
                conflictId,
                new MutationError("entity_deleted", "A alteração foi preservada para recuperação."));
    }

    private MutationResult parentDeleted(EntityMutationCommand command, SyncEntityHead parent) {
        UUID conflictId = conflictRegistrar.registerParentDeleted(command, parent);
        return failed(command.operation(), command.order(), "parent_deleted", conflictId);
    }

    private MutationResult deleted(EntityMutationCommand command, SyncEntityHead head) {
        if (!isDelete(command.operation())) return resolveConcurrent(command, head);
        return new MutationResult(
                command.operation().operationId(),
                MutationOutcome.APPLIED,
                head.version(),
                null,
                command.order(),
                null,
                null);
    }

    private MutationResult failed(SyncMutationOperation operation, EventOrder order, String code, UUID conflictId) {
        return new MutationResult(
                operation.operationId(),
                MutationOutcome.ACTION_REQUIRED,
                null,
                null,
                order,
                conflictId,
                new MutationError(code, "A alteração precisa de atenção."));
    }

    private MutationResult rejected(EntityMutationCommand command, String code) {
        return new MutationResult(
                command.operation().operationId(),
                MutationOutcome.ACTION_REQUIRED,
                null,
                null,
                command.order(),
                null,
                new MutationError(code, "A alteração precisa de atenção."));
    }

    private String typeOf(SyncMutationOperation operation) {
        return operation.kind().value().startsWith("deck_") ? "deck" : "card";
    }

    private boolean isDelete(SyncMutationOperation operation) {
        return operation.kind().value().endsWith("_delete");
    }

    private boolean isRestore(SyncMutationOperation operation) {
        return operation.kind().value().equals("conflict_restore");
    }

    private boolean isDeckDelete(SyncMutationOperation operation) {
        return operation.kind().value().equals("deck_delete");
    }
}
