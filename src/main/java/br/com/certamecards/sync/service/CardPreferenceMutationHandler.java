package br.com.certamecards.sync.service;

import br.com.certamecards.card.service.CardService;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.service.DeckProgressResetter;
import br.com.certamecards.deck.service.ResetProgressResult;
import br.com.certamecards.sync.domain.EventOrder;
import br.com.certamecards.sync.domain.MutationError;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CardPreferenceMutationHandler implements SyncMutationHandler {

    private final CardService cardService;
    private final DeckProgressResetter progressResetter;
    private final MutationPayloadReader payloadReader;

    public CardPreferenceMutationHandler(
            CardService cardService, DeckProgressResetter progressResetter, MutationPayloadReader payloadReader) {
        this.cardService = cardService;
        this.progressResetter = progressResetter;
        this.payloadReader = payloadReader;
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        EventOrder order = context.order();
        return switch (operation.kind()) {
            case CARD_SUSPENSION -> suspend(userId, operation, order);
            case DECK_RESET -> reset(userId, operation, order);
            default -> throw new IllegalArgumentException("Unsupported card preference mutation");
        };
    }

    private MutationResult suspend(UUID userId, SyncMutationOperation operation, EventOrder order) {
        CardPreferencePayload payload = payloadReader.cardPreference(operation);
        try {
            var state = cardService.setSuspension(userId, operation.entityId(), payload.suspended());
            return new MutationResult(
                    operation.operationId(), MutationOutcome.APPLIED, null, state.getChangeSeq(), order, null, null);
        } catch (ApiException exception) {
            return notApplicable(operation, order, exception);
        }
    }

    private MutationResult reset(UUID userId, SyncMutationOperation operation, EventOrder order) {
        try {
            ResetProgressResult result = progressResetter.reset(userId, operation.entityId());
            return new MutationResult(
                    operation.operationId(), MutationOutcome.APPLIED, null, result.cursorHint(), order, null, null);
        } catch (ApiException exception) {
            return notApplicable(operation, order, exception);
        }
    }

    private MutationResult notApplicable(SyncMutationOperation operation, EventOrder order, ApiException exception) {
        if (exception.getErrorCode() != ErrorCode.NOT_FOUND) {
            throw exception;
        }
        return new MutationResult(
                operation.operationId(),
                MutationOutcome.ACTION_REQUIRED,
                null,
                null,
                order,
                null,
                new MutationError("not_applicable", "O objeto deixou de estar disponível."));
    }
}
