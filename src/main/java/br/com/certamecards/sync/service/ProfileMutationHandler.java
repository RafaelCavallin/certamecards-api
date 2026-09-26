package br.com.certamecards.sync.service;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.persistence.SettingFieldClockRepository;
import br.com.certamecards.user.service.AccountService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProfileMutationHandler implements SyncMutationHandler {

    private final AccountService accountService;
    private final MutationPayloadReader payloadReader;
    private final SettingFieldClockRepository clocks;

    public ProfileMutationHandler(
            AccountService accountService, MutationPayloadReader payloadReader, SettingFieldClockRepository clocks) {
        this.accountService = accountService;
        this.payloadReader = payloadReader;
        this.clocks = clocks;
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        EventOrder order = context.order();
        ProfilePatchPayload payload = payloadReader.profile(operation);
        if (payload.displayName() != null && clocks.advance(userId, "displayName", order)) {
            accountService.updateDisplayName(userId, payload.displayName());
        }
        return new MutationResult(operation.operationId(), MutationOutcome.APPLIED, null, null, order, null, null);
    }
}
