package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.sync.domain.MutationResult;
import org.springframework.stereotype.Service;

@Service
public class PreferenceMutationCoordinator {

    private final PreferenceMutationWriter writer;

    public PreferenceMutationCoordinator(PreferenceMutationWriter writer) {
        this.writer = writer;
    }

    public MutationResult attempt(EntityMutationCommand command) {
        try {
            return writer.attempt(command);
        } catch (ApiException exception) {
            return writer.recordRejection(command, codeFor(exception));
        } catch (IllegalArgumentException exception) {
            return writer.recordRejection(command, "validation_failed");
        }
    }

    private String codeFor(ApiException exception) {
        return exception.getErrorCode() == ErrorCode.NOT_FOUND
                ? "not_applicable"
                : exception.getErrorCode().code();
    }
}
