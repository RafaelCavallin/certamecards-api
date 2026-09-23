package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.SyncMutationOperation;
import org.springframework.stereotype.Component;

@Component
public class MutationHandlerRegistry {

    private final DeckMutationHandler deckHandler;
    private final CardMutationHandler cardHandler;
    private final CardPreferenceMutationHandler preferenceHandler;
    private final SettingsMutationHandler settingsHandler;
    private final ProfileMutationHandler profileHandler;

    public MutationHandlerRegistry(
            DeckMutationHandler deckHandler,
            CardMutationHandler cardHandler,
            CardPreferenceMutationHandler preferenceHandler,
            SettingsMutationHandler settingsHandler,
            ProfileMutationHandler profileHandler) {
        this.deckHandler = deckHandler;
        this.cardHandler = cardHandler;
        this.preferenceHandler = preferenceHandler;
        this.settingsHandler = settingsHandler;
        this.profileHandler = profileHandler;
    }

    public SyncMutationHandler forOperation(SyncMutationOperation operation) {
        return switch (operation.kind()) {
            case DECK_CREATE, DECK_UPDATE, DECK_DELETE -> deckHandler;
            case CARD_CREATE, CARD_UPDATE, CARD_DELETE -> cardHandler;
            case CARD_SUSPENSION, DECK_RESET -> preferenceHandler;
            case SETTINGS_PATCH -> settingsHandler;
            case PROFILE_PATCH -> profileHandler;
            default -> this::notApplicable;
        };
    }

    public boolean needsEntityHead(SyncMutationOperation operation) {
        return switch (operation.kind()) {
            case DECK_CREATE, DECK_UPDATE, DECK_DELETE, CARD_CREATE, CARD_UPDATE, CARD_DELETE -> true;
            default -> false;
        };
    }

    private br.com.certamecards.sync.domain.MutationResult notApplicable(
            java.util.UUID userId, MutationContext context) {
        return new br.com.certamecards.sync.domain.MutationResult(
                context.operation().operationId(),
                br.com.certamecards.sync.domain.MutationOutcome.ACTION_REQUIRED,
                null,
                null,
                context.order(),
                null,
                new br.com.certamecards.sync.domain.MutationError("not_applicable", "A alteração precisa de atenção."));
    }
}
