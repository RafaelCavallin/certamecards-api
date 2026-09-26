package br.com.certamecards.sync.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.CreateDeckCommand;
import br.com.certamecards.deck.service.DeckContent;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.deck.service.UpdateDeckCommand;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class DeckMutationHandler implements SyncMutationHandler {

    private final DeckService deckService;
    private final MutationPayloadReader payloadReader;
    private final ObjectMapper objectMapper;

    public DeckMutationHandler(
            DeckService deckService, MutationPayloadReader payloadReader, ObjectMapper objectMapper) {
        this.deckService = deckService;
        this.payloadReader = payloadReader;
        this.objectMapper = objectMapper;
    }

    @Override
    public String currentSnapshot(UUID userId, UUID entityId) {
        try {
            Deck deck = deckService.findOwned(userId, entityId);
            return objectMapper.writeValueAsString(
                    new DeckMutationPayload(deck.getSubjectId(), deck.getName(), deck.getDescription()));
        } catch (ApiException exception) {
            return "{}";
        }
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        return switch (operation.kind()) {
            case DECK_CREATE -> create(userId, operation, context.order());
            case DECK_UPDATE -> update(userId, context);
            case DECK_DELETE -> delete(userId, context);
            default -> throw new IllegalArgumentException("Unsupported deck mutation");
        };
    }

    private MutationResult create(UUID userId, SyncMutationOperation operation, EventOrder order) {
        DeckMutationPayload payload = payloadReader.deck(operation);
        if (payload.subjectId() == null) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
        Deck deck = deckService
                .create(new CreateDeckCommand(
                        operation.entityId(),
                        userId,
                        payload.subjectId(),
                        new DeckContent(payload.name(), payload.description())))
                .deck();
        return result(operation, order, deck);
    }

    private MutationResult update(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        DeckMutationPayload payload = payloadReader.deck(operation);
        Deck deck = deckService.update(
                userId,
                operation.entityId(),
                new UpdateDeckCommand(
                        payload.subjectId(), new DeckContent(payload.name(), payload.description()), version(context)));
        return result(operation, context.order(), deck);
    }

    private MutationResult delete(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        deckService.delete(userId, operation.entityId(), version(context));
        return new MutationResult(
                operation.operationId(), MutationOutcome.APPLIED, null, null, context.order(), null, null);
    }

    private int version(MutationContext context) {
        if (context.currentVersion() == null) throw new IllegalArgumentException("baseVersion is required");
        return context.currentVersion();
    }

    private MutationResult result(SyncMutationOperation operation, EventOrder order, Deck deck) {
        return new MutationResult(
                operation.operationId(),
                MutationOutcome.APPLIED,
                deck.getVersion(),
                deck.getChangeSeq(),
                order,
                null,
                null);
    }
}
