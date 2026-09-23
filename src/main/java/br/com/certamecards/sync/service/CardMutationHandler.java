package br.com.certamecards.sync.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardContent;
import br.com.certamecards.card.service.CardService;
import br.com.certamecards.card.service.CreateCardCommand;
import br.com.certamecards.card.service.UpdateCardCommand;
import br.com.certamecards.sync.domain.EventOrder;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CardMutationHandler implements SyncMutationHandler {

    private final CardService cardService;
    private final MutationPayloadReader payloadReader;

    public CardMutationHandler(CardService cardService, MutationPayloadReader payloadReader) {
        this.cardService = cardService;
        this.payloadReader = payloadReader;
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        return switch (operation.kind()) {
            case CARD_CREATE -> create(userId, operation, context.order());
            case CARD_UPDATE -> update(userId, context);
            case CARD_DELETE -> delete(userId, context);
            default -> throw new IllegalArgumentException("Unsupported card mutation");
        };
    }

    private MutationResult create(UUID userId, SyncMutationOperation operation, EventOrder order) {
        CardMutationPayload payload = payloadReader.card(operation);
        if (operation.parentId() == null) throw new IllegalArgumentException("parentId is required");
        Card card = cardService
                .create(new CreateCardCommand(
                        operation.entityId(),
                        operation.parentId(),
                        userId,
                        new CardContent(payload.front(), payload.back(), payload.source())))
                .card();
        return result(operation, order, card);
    }

    private MutationResult update(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        CardMutationPayload payload = payloadReader.card(operation);
        Card card = cardService.update(
                userId,
                operation.entityId(),
                new UpdateCardCommand(
                        new CardContent(payload.front(), payload.back(), payload.source()), version(context)));
        return result(operation, context.order(), card);
    }

    private MutationResult delete(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        cardService.delete(userId, operation.entityId(), version(context));
        return new MutationResult(
                operation.operationId(), MutationOutcome.APPLIED, null, null, context.order(), null, null);
    }

    private int version(MutationContext context) {
        if (context.currentVersion() == null) throw new IllegalArgumentException("baseVersion is required");
        return context.currentVersion();
    }

    private MutationResult result(SyncMutationOperation operation, EventOrder order, Card card) {
        return new MutationResult(
                operation.operationId(),
                MutationOutcome.APPLIED,
                card.getVersion(),
                card.getChangeSeq(),
                order,
                null,
                null);
    }
}
