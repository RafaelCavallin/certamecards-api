package br.com.certamecards.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.certamecards.common.sync.EventClock;
import br.com.certamecards.sync.domain.SyncMutationOperation;
import br.com.certamecards.sync.domain.SyncOperationKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.node.JsonNodeFactory;

class MutationHandlerRegistryTest {

    private static final Instant NOW = Instant.parse("2026-09-22T12:00:00Z");
    private final DeckMutationHandler deckHandler = mock(DeckMutationHandler.class);
    private final CardMutationHandler cardHandler = mock(CardMutationHandler.class);
    private final CardPreferenceMutationHandler preferenceHandler = mock(CardPreferenceMutationHandler.class);
    private final DeckResetHandler resetHandler = mock(DeckResetHandler.class);
    private final SettingsMutationHandler settingsHandler = mock(SettingsMutationHandler.class);
    private final ProfileMutationHandler profileHandler = mock(ProfileMutationHandler.class);
    private final ConflictRestoreHandler conflictRestoreHandler = mock(ConflictRestoreHandler.class);
    private final MutationHandlerRegistry registry = new MutationHandlerRegistry(
            deckHandler,
            cardHandler,
            preferenceHandler,
            resetHandler,
            settingsHandler,
            profileHandler,
            conflictRestoreHandler);

    @ParameterizedTest
    @EnumSource(
            value = SyncOperationKind.class,
            names = {"DECK_CREATE", "DECK_UPDATE", "DECK_DELETE"})
    void givenDeckOperation_whenResolving_thenReturnsDeckHandler(SyncOperationKind kind) {
        assertThat(registry.forOperation(operation(kind))).isSameAs(deckHandler);
    }

    @ParameterizedTest
    @EnumSource(
            value = SyncOperationKind.class,
            names = {"CARD_CREATE", "CARD_UPDATE", "CARD_DELETE"})
    void givenCardOperation_whenResolving_thenReturnsCardHandler(SyncOperationKind kind) {
        assertThat(registry.forOperation(operation(kind))).isSameAs(cardHandler);
    }

    @Test
    void givenSuspensionOperation_whenResolving_thenReturnsPreferenceHandler() {
        assertThat(registry.forOperation(operation(SyncOperationKind.CARD_SUSPENSION)))
                .isSameAs(preferenceHandler);
    }

    @Test
    void givenResetOperation_whenResolving_thenReturnsResetHandler() {
        assertThat(registry.forOperation(operation(SyncOperationKind.DECK_RESET)))
                .isSameAs(resetHandler);
    }

    @Test
    void givenSettingsOperation_whenResolving_thenReturnsSettingsHandler() {
        assertThat(registry.forOperation(operation(SyncOperationKind.SETTINGS_PATCH)))
                .isSameAs(settingsHandler);
    }

    @Test
    void givenProfileOperation_whenResolving_thenReturnsProfileHandler() {
        assertThat(registry.forOperation(operation(SyncOperationKind.PROFILE_PATCH)))
                .isSameAs(profileHandler);
    }

    @Test
    void givenConflictRestoreOperation_whenResolving_thenReturnsConflictRestoreHandler() {
        assertThat(registry.forOperation(operation(SyncOperationKind.CONFLICT_RESTORE)))
                .isSameAs(conflictRestoreHandler);
    }

    @ParameterizedTest
    @EnumSource(
            value = SyncOperationKind.class,
            names = {
                "DECK_CREATE",
                "DECK_UPDATE",
                "DECK_DELETE",
                "CARD_CREATE",
                "CARD_UPDATE",
                "CARD_DELETE",
                "CONFLICT_RESTORE"
            })
    void givenEntityOperation_whenCheckingHead_thenNeedsHead(SyncOperationKind kind) {
        assertThat(registry.needsEntityHead(operation(kind))).isTrue();
    }

    @ParameterizedTest
    @EnumSource(
            value = SyncOperationKind.class,
            names = {"CARD_SUSPENSION", "DECK_RESET", "SETTINGS_PATCH", "PROFILE_PATCH"})
    void givenPreferenceOperation_whenCheckingHead_thenDoesNotNeedHead(SyncOperationKind kind) {
        assertThat(registry.needsEntityHead(operation(kind))).isFalse();
    }

    private SyncMutationOperation operation(SyncOperationKind kind) {
        return new SyncMutationOperation(
                UUID.randomUUID(),
                kind,
                UUID.randomUUID(),
                null,
                null,
                null,
                List.of(),
                NOW,
                new EventClock(NOW, 0),
                NOW,
                JsonNodeFactory.instance.objectNode());
    }
}
