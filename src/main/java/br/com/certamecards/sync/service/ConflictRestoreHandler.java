package br.com.certamecards.sync.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.service.CardContent;
import br.com.certamecards.card.service.CardService;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.DeckContent;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.deck.service.UpdateDeckCommand;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncConflict;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConflictRestoreHandler implements SyncMutationHandler {

    private final SyncConflictService conflictService;
    private final MutationPayloadReader payloadReader;
    private final DeckService deckService;
    private final CardService cardService;

    public ConflictRestoreHandler(
            SyncConflictService conflictService,
            MutationPayloadReader payloadReader,
            DeckService deckService,
            CardService cardService) {
        this.conflictService = conflictService;
        this.payloadReader = payloadReader;
        this.deckService = deckService;
        this.cardService = cardService;
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        ConflictRestorePayload payload = payloadReader.conflictRestore(operation);
        SyncConflict conflict = conflictService.forRestore(userId, payload.conflictId());
        if (!conflict.entityId().equals(operation.entityId())) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
        MutationResult result = "deck".equals(conflict.entityType())
                ? restoreDeck(userId, operation, context, payload)
                : restoreCard(userId, operation, context, conflict, payload);
        conflictService.markRestored(conflict.id(), context.order().eventAt());
        return result;
    }

    private MutationResult restoreDeck(
            UUID userId, SyncMutationOperation operation, MutationContext context, ConflictRestorePayload payload) {
        var content = payloadReader.deckSnapshot(payload.snapshot());
        Deck deck = deckService.restore(
                userId,
                operation.entityId(),
                new UpdateDeckCommand(content.subjectId(), new DeckContent(content.name(), content.description()), 0));
        return applied(operation, context, deck.getVersion(), deck.getChangeSeq());
    }

    private MutationResult restoreCard(
            UUID userId,
            SyncMutationOperation operation,
            MutationContext context,
            SyncConflict conflict,
            ConflictRestorePayload payload) {
        var content = payloadReader.cardSnapshot(payload.snapshot());
        UUID targetDeckId = payload.targetDeckId() != null ? payload.targetDeckId() : conflict.deckId();
        Card card = cardService.restore(
                userId,
                operation.entityId(),
                targetDeckId,
                new CardContent(content.front(), content.back(), content.source()));
        return applied(operation, context, card.getVersion(), card.getChangeSeq());
    }

    private MutationResult applied(
            SyncMutationOperation operation, MutationContext context, int version, Long changeSeq) {
        return new MutationResult(
                operation.operationId(), MutationOutcome.APPLIED, version, changeSeq, context.order(), null, null);
    }
}
