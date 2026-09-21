package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.DeckCreationResult;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.subject.service.SubjectLookup;
import br.com.certamecards.support.MutableClock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialDeckServiceTest {

    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final SubjectLookup subjectLookup = mock(SubjectLookup.class);
    private final OfficialDeckUpdateApplier updateApplier = mock(OfficialDeckUpdateApplier.class);
    private final OfficialDeckAuditRecorder auditRecorder = mock(OfficialDeckAuditRecorder.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-19T14:00:00Z"), ZoneOffset.UTC);
    private final OfficialDeckService service =
            new OfficialDeckService(deckAccess, subjectLookup, updateApplier, auditRecorder, clock);
    private final UUID actorId = UUID.randomUUID();

    @Test
    void givenNewId_whenCreating_thenBuildsDraftDeckAndRecordsAudit() {
        UUID id = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        when(deckAccess.findById(id)).thenReturn(null);
        when(deckAccess.save(any(Deck.class))).thenAnswer(call -> call.getArgument(0));

        DeckCreationResult result =
                service.create(actorId, new CreateOfficialDeckCommand(id, subjectId, "CF/88", null));

        assertThat(result.created()).isTrue();
        assertThat(result.deck().getOrigin()).isEqualTo(DeckOrigin.OFFICIAL_SUBSCRIPTION);
        assertThat(result.deck().getOwnerId()).isNull();
        assertThat(result.deck().getOfficialMeta().getOfficialStatus()).isEqualTo("draft");
        verify(subjectLookup).requireActive(subjectId);
        verify(auditRecorder).recordCreated(actorId, result.deck());
    }

    @Test
    void givenExistingId_whenCreating_thenIsIdempotentAndSkipsAudit() {
        UUID id = UUID.randomUUID();
        Deck existing = new Deck(id, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        when(deckAccess.findById(id)).thenReturn(existing);

        DeckCreationResult result =
                service.create(actorId, new CreateOfficialDeckCommand(id, UUID.randomUUID(), "CF/88", null));

        assertThat(result.created()).isFalse();
        assertThat(result.deck()).isSameAs(existing);
    }

    @Test
    void givenMismatchedVersion_whenUpdating_thenThrowsVersionConflict() {
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);

        assertThatThrownBy(() -> service.update(actorId, deckId, new UpdateOfficialDeckCommand(null, null, null, 9)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    @Test
    void givenMatchingVersion_whenUpdating_thenAppliesChangesAndTouchesContent() {
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        UpdateOfficialDeckCommand command = new UpdateOfficialDeckCommand(null, "Novo nome", null, 0);
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);
        when(updateApplier.apply(deck, command)).thenReturn(Map.of());
        when(deckAccess.save(deck)).thenReturn(deck);

        Deck saved = service.update(actorId, deckId, command);

        assertThat(saved.getOfficialMeta().getContentUpdatedAt()).isEqualTo(clock.instant());
        verify(auditRecorder).recordUpdated(actorId, deck, Map.of());
    }

    @Test
    void whenFindingOfficial_thenDelegatesToAccess() {
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        when(deckAccess.findOfficial(deckId)).thenReturn(deck);

        assertThat(service.findOfficial(deckId)).isSameAs(deck);
    }
}
