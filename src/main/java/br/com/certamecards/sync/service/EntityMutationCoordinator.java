package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.sync.domain.MutationResult;
import org.springframework.stereotype.Service;

@Service
public class EntityMutationCoordinator {

    private final MutationReceiptService receipts;
    private final EntityMutationWriter writer;

    public EntityMutationCoordinator(MutationReceiptService receipts, EntityMutationWriter writer) {
        this.receipts = receipts;
        this.writer = writer;
    }

    public MutationResult execute(EntityMutationCommand command) {
        var existing = receipts.find(command.userId(), command.operation().operationId());
        if (existing.isPresent()) return writer.duplicate(existing.get(), command.requestHash());
        return attempt(command);
    }

    private MutationResult attempt(EntityMutationCommand command) {
        try {
            return writer.attempt(command);
        } catch (ApiException exception) {
            return writer.recordRejection(command, exception.getErrorCode().code());
        } catch (IllegalArgumentException exception) {
            return writer.recordRejection(command, "validation_failed");
        }
    }
}
