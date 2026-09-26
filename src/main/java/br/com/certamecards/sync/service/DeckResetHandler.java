package br.com.certamecards.sync.service;

import br.com.certamecards.common.sync.EventOrder;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.sync.domain.MutationOutcome;
import br.com.certamecards.sync.domain.MutationResult;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.persistence.DeckResetLogWriter;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DeckResetHandler implements SyncMutationHandler {

    private final DeckService deckService;
    private final DeckResetLogWriter resetLogWriter;
    private final Clock clock;

    public DeckResetHandler(DeckService deckService, DeckResetLogWriter resetLogWriter, Clock clock) {
        this.deckService = deckService;
        this.resetLogWriter = resetLogWriter;
        this.clock = clock;
    }

    @Override
    public MutationResult handle(UUID userId, MutationContext context) {
        SyncMutationOperation operation = context.operation();
        EventOrder order = context.order();
        deckService.findAccessible(userId, operation.entityId());
        resetLogWriter.materialize(userId, operation.entityId(), order, clock.instant());
        return new MutationResult(operation.operationId(), MutationOutcome.APPLIED, null, null, order, null, null);
    }
}
