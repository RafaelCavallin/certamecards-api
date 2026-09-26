package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.SyncMutationOperation;
import org.springframework.stereotype.Component;

@Component
public class MutationHandlerRegistry {

    private final DeckMutationHandler deckHandler;
    private final CardMutationHandler cardHandler;
    private final CardPreferenceMutationHandler preferenceHandler;
    private final DeckResetHandler resetHandler;
    private final SettingsMutationHandler settingsHandler;
    private final ProfileMutationHandler profileHandler;
    private final ConflictRestoreHandler conflictRestoreHandler;

    public MutationHandlerRegistry(
            DeckMutationHandler deckHandler,
            CardMutationHandler cardHandler,
            CardPreferenceMutationHandler preferenceHandler,
            DeckResetHandler resetHandler,
            SettingsMutationHandler settingsHandler,
            ProfileMutationHandler profileHandler,
            ConflictRestoreHandler conflictRestoreHandler) {
        this.deckHandler = deckHandler;
        this.cardHandler = cardHandler;
        this.preferenceHandler = preferenceHandler;
        this.resetHandler = resetHandler;
        this.settingsHandler = settingsHandler;
        this.profileHandler = profileHandler;
        this.conflictRestoreHandler = conflictRestoreHandler;
    }

    public SyncMutationHandler forOperation(SyncMutationOperation operation) {
        return switch (operation.kind()) {
            case DECK_CREATE, DECK_UPDATE, DECK_DELETE -> deckHandler;
            case CARD_CREATE, CARD_UPDATE, CARD_DELETE -> cardHandler;
            case CARD_SUSPENSION -> preferenceHandler;
            case DECK_RESET -> resetHandler;
            case SETTINGS_PATCH -> settingsHandler;
            case PROFILE_PATCH -> profileHandler;
            case CONFLICT_RESTORE -> conflictRestoreHandler;
        };
    }

    public boolean needsEntityHead(SyncMutationOperation operation) {
        return switch (operation.kind()) {
            case DECK_CREATE, DECK_UPDATE, DECK_DELETE, CARD_CREATE, CARD_UPDATE, CARD_DELETE, CONFLICT_RESTORE -> true;
            default -> false;
        };
    }
}
