package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.MutationError;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PreferenceMutationWriter {

    private final MutationReceiptService receipts;

    public PreferenceMutationWriter(MutationReceiptService receipts) {
        this.receipts = receipts;
    }

    @Transactional
    public MutationResult attempt(EntityMutationCommand command) {
        MutationResult result = command.handler()
                .handle(command.userId(), new MutationContext(command.operation(), command.order(), null));
        return reserve(command, result);
    }

    @Transactional
    public MutationResult recordRejection(EntityMutationCommand command, String code) {
        MutationResult result = new MutationResult(
                command.operation().operationId(),
                MutationOutcome.ACTION_REQUIRED,
                null,
                null,
                command.order(),
                null,
                new MutationError(code, "A alteração precisa de atenção."));
        return reserve(command, result);
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
}
