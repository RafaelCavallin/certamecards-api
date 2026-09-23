package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.SyncLimits;
import br.com.certamecards.sync.domain.SyncMutationBatch;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import org.springframework.stereotype.Component;

@Component
public class MutationBatchValidator {

    public void validate(SyncMutationBatch batch) {
        if (batch.deviceId() == null
                || batch.operations() == null
                || batch.operations().isEmpty()) invalid();
        if (batch.operations().size() > SyncLimits.MAX_MUTATION_BATCH_SIZE) invalid();
        batch.operations().forEach(this::validateOperation);
    }

    private void validateOperation(SyncMutationOperation operation) {
        if (operation.operationId() == null || operation.kind() == null || operation.entityId() == null) invalid();
        if (operation.dependsOn() == null || operation.dependsOn().size() > SyncLimits.MAX_MUTATION_DEPENDENCIES)
            invalid();
        if (operation.clock() == null || operation.clock().wallTime() == null || operation.payload() == null) invalid();
    }

    private void invalid() {
        throw ApiException.of(ErrorCode.VALIDATION_FAILED);
    }
}
