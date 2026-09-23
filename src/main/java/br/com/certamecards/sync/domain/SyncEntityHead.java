package br.com.certamecards.sync.domain;

import java.util.UUID;

public record SyncEntityHead(
        UUID userId,
        String entityType,
        UUID entityId,
        UUID parentId,
        int version,
        UUID winningOperationId,
        EventOrder order,
        boolean deleted) {

    public boolean isCausalSuccessor(SyncMutationOperation operation) {
        return winningOperationId != null && winningOperationId.equals(operation.predecessorOperationId());
    }

    public boolean hasBaseVersion(SyncMutationOperation operation) {
        return operation.baseVersion() != null && version == operation.baseVersion();
    }
}
