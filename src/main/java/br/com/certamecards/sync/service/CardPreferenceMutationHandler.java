package br.com.certamecards.sync.service;

import br.com.certamecards.card.service.CardService;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CardPreferenceMutationHandler implements SyncMutationHandler {

    private final CardService cardService;
    private final MutationPayloadReader payloadReader;

    public CardPreferenceMutationHandler(CardService cardService, MutationPayloadReader payloadReader) {
        this.cardService = cardService;
        this.payloadReader = payloadReader;
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        EventOrder order = context.order();
        return switch (operation.kind()) {
            case CARD_SUSPENSION -> suspend(userId, operation, order);
            default -> throw new IllegalArgumentException("Unsupported card preference mutation");
        };
    }

    private MutationResult suspend(UUID userId, SyncMutationOperation operation, EventOrder order) {
        CardPreferencePayload payload = payloadReader.cardPreference(operation);
        var state = cardService.setSuspension(userId, operation.entityId(), payload.suspended());
        return new MutationResult(
                operation.operationId(), MutationOutcome.APPLIED, null, state.getChangeSeq(), order, null, null);
    }
}
