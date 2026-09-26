package br.com.certamecards.deck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.persistence.DeckCardsQuery;
import br.com.certamecards.deck.persistence.DeckRepository;
import br.com.certamecards.subject.domain.Subject;
import br.com.certamecards.subject.service.SubjectLookup;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeckServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final DeckRepository deckRepository = mock(DeckRepository.class);
    private final SubjectLookup subjectLookup = mock(SubjectLookup.class);
    private final DeckCardsQuery deckCardsQuery = mock(DeckCardsQuery.class);
    private final DeckUpdateApplier updateApplier = new DeckUpdateApplier(subjectLookup);
    private final DeckService deckService =
            new DeckService(deckRepository, subjectLookup, deckCardsQuery, updateApplier, FIXED_CLOCK);

    @Test
    void givenSubjectWithActiveDecks_whenCountingActiveBySubject_thenDelegatesToRepository() {
        UUID subjectId = UUID.randomUUID();
        when(deckRepository.countBySubjectIdAndAudit_DeletedAtIsNull(subjectId)).thenReturn(3L);
        assertThat(deckService.countActiveBySubject(subjectId)).isEqualTo(3L);
    }

    @Test
    @DisplayName("TU-31 — criar deck com matéria inativa lança subject_inactive")
    void givenInactiveSubject_whenCreating_thenThrowsSubjectInactive() {
        UUID subjectId = UUID.randomUUID();
        when(deckRepository.findById(any())).thenReturn(Optional.empty());
        when(subjectLookup.requireActive(subjectId)).thenThrow(ApiException.of(ErrorCode.SUBJECT_INACTIVE));
        CreateDeckCommand command =
                new CreateDeckCommand(UUID.randomUUID(), UUID.randomUUID(), subjectId, new DeckContent("CF/88", null));

        assertThatThrownBy(() -> deckService.create(command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.SUBJECT_INACTIVE));
    }

    @Test
    void givenSameIdAndOwner_whenCreatingTwice_thenSecondCallIsIdempotent() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        Deck existing = new Deck(id, ownerId, subjectId, "CF/88");
        when(deckRepository.findById(id)).thenReturn(Optional.of(existing));
        CreateDeckCommand command = new CreateDeckCommand(id, ownerId, subjectId, new DeckContent("CF/88", null));

        DeckCreationResult result = deckService.create(command);

        assertThat(result.created()).isFalse();
        assertThat(result.deck()).isSameAs(existing);
        verifyNoInteractions(subjectLookup);
    }

    @Test
    @DisplayName("TU-31 — editar mantendo a matéria inativa sem trocá-la é permitido")
    void givenActiveInactiveSubjectUnchanged_whenRenamingOnly_thenSucceedsWithoutSubjectCheck() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, UUID.randomUUID(), "Antigo");
        when(deckRepository.findByIdAndOwnerId(deckId, ownerId)).thenReturn(Optional.of(deck));
        when(deckRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        UpdateDeckCommand command = new UpdateDeckCommand(null, new DeckContent("Novo nome", null), 0);

        Deck updated = deckService.update(ownerId, deckId, command);

        assertThat(updated.getName()).isEqualTo("Novo nome");
        verifyNoInteractions(subjectLookup);
    }

    @Test
    void givenMismatchedVersion_whenUpdating_thenThrowsVersionConflict() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, UUID.randomUUID(), "Antigo");
        when(deckRepository.findByIdAndOwnerId(deckId, ownerId)).thenReturn(Optional.of(deck));
        UpdateDeckCommand command = new UpdateDeckCommand(null, new DeckContent("Novo nome", null), 5);

        assertThatThrownBy(() -> deckService.update(ownerId, deckId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    @Test
    void givenDeletedDeck_whenRestoring_thenClearsDeletionAndAppliesContent() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, subjectId, "Antigo");
        deck.markDeleted(FIXED_NOW.minusSeconds(60));
        when(deckRepository.findByIdAndOwnerId(deckId, ownerId)).thenReturn(Optional.of(deck));
        when(deckRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        UpdateDeckCommand command = new UpdateDeckCommand(subjectId, new DeckContent("Restaurado", null), 0);

        Deck restored = deckService.restore(ownerId, deckId, command);

        assertThat(restored.isDeleted()).isFalse();
        assertThat(restored.getName()).isEqualTo("Restaurado");
    }

    @Test
    void givenOwnedDeck_whenDeleting_thenMarksDeletedAndCascadesToCards() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, UUID.randomUUID(), "Antigo");
        when(deckRepository.findByIdAndOwnerId(deckId, ownerId)).thenReturn(Optional.of(deck));
        when(deckRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        deckService.delete(ownerId, deckId, 0);

        assertThat(deck.isDeleted()).isTrue();
    }

    @Test
    void givenSubject_whenTouched_thenAppliesGivenSubject() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID newSubjectId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, UUID.randomUUID(), "Antigo");
        when(deckRepository.findByIdAndOwnerId(deckId, ownerId)).thenReturn(Optional.of(deck));
        when(deckRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(subjectLookup.requireActive(newSubjectId)).thenReturn(new Subject("Nova", "nova"));
        UpdateDeckCommand command = new UpdateDeckCommand(newSubjectId, null, 0);

        Deck updated = deckService.update(ownerId, deckId, command);

        assertThat(updated.getSubjectId()).isEqualTo(newSubjectId);
    }
}
