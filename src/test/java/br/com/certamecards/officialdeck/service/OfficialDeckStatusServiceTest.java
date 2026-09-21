package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.officialdeck.domain.OfficialDeckDeletionGuard;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatusTransitions;
import br.com.certamecards.support.MutableClock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialDeckStatusServiceTest {

    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final OfficialDeckStatusTransitions statusTransitions = mock(OfficialDeckStatusTransitions.class);
    private final OfficialDeckDeletionGuard deletionGuard = mock(OfficialDeckDeletionGuard.class);
    private final OfficialDeckAuditRecorder auditRecorder = mock(OfficialDeckAuditRecorder.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-19T14:00:00Z"), ZoneOffset.UTC);
    private final OfficialDeckStatusService service =
            new OfficialDeckStatusService(deckAccess, statusTransitions, deletionGuard, auditRecorder, clock);
    private final UUID actorId = UUID.randomUUID();

    @Test
    void givenMismatchedVersion_whenChangingStatus_thenThrowsVersionConflict() {
        UUID deckId = UUID.randomUUID();
        Deck deck = draftDeck(deckId);
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);

        assertThatThrownBy(() -> service.changeStatus(actorId, deckId, OfficialDeckStatus.PUBLISHED, 9))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    @Test
    void givenValidTransition_whenChangingStatus_thenPersistsAndRecordsAudit() {
        UUID deckId = UUID.randomUUID();
        Deck deck = draftDeck(deckId);
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);
        when(deckAccess.save(deck)).thenReturn(deck);

        Deck saved = service.changeStatus(actorId, deckId, OfficialDeckStatus.PUBLISHED, 0);

        assertThat(saved.getOfficialMeta().getOfficialStatus()).isEqualTo("published");
        verify(auditRecorder)
                .recordStatusChanged(actorId, deck, OfficialDeckStatus.DRAFT, OfficialDeckStatus.PUBLISHED);
    }

    @Test
    void givenDeletableDeck_whenDeleting_thenMarksDeletedAndRecordsAudit() {
        UUID deckId = UUID.randomUUID();
        Deck deck = draftDeck(deckId);
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);

        service.delete(actorId, deckId, 0);

        assertThat(deck.isDeleted()).isTrue();
        verify(auditRecorder).recordDeleted(actorId, deckId, deck.getName());
    }

    @Test
    void givenGuardRejects_whenDeleting_thenPropagatesExceptionAndDoesNotDelete() {
        UUID deckId = UUID.randomUUID();
        Deck deck = draftDeck(deckId);
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);
        org.mockito.Mockito.doThrow(ApiException.of(ErrorCode.OFFICIAL_DECK_HAS_SUBSCRIBERS))
                .when(deletionGuard)
                .ensureDeletable(deckId);

        assertThatThrownBy(() -> service.delete(actorId, deckId, 0)).isInstanceOf(ApiException.class);
        assertThat(deck.isDeleted()).isFalse();
        verify(auditRecorder, never()).recordDeleted(any(), any(), any());
    }

    private Deck draftDeck(UUID deckId) {
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        deck.getOfficialMeta().markDraft(clock.instant());
        return deck;
    }
}
