package br.com.certamecards.subject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.subject.domain.Subject;
import br.com.certamecards.subject.persistence.SubjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubjectServiceTest {

    private final UUID actorId = UUID.randomUUID();
    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final DeckService deckService = mock(DeckService.class);
    private final SubjectAuditRecorder auditRecorder = mock(SubjectAuditRecorder.class);
    private final SubjectService subjectService = new SubjectService(subjectRepository, deckService, auditRecorder);

    @Test
    void givenFreeName_whenCreating_thenSavesNormalizedNameAndZeroDeckCountAndRecordsAudit() {
        when(subjectRepository.findByNormalizedName("direito constitucional")).thenReturn(Optional.empty());
        when(subjectRepository.saveAndFlush(any(Subject.class))).thenAnswer(call -> call.getArgument(0));

        SubjectWithDeckCount result = subjectService.create(actorId, "Direito Constitucional");

        assertThat(result.subject().getName()).isEqualTo("Direito Constitucional");
        assertThat(result.subject().getNormalizedName()).isEqualTo("direito constitucional");
        assertThat(result.deckCount()).isZero();
        verify(auditRecorder).recordCreated(eq(actorId), any(Subject.class));
    }

    @Test
    void givenNameAlreadyTaken_whenCreating_thenThrowsSubjectNameTaken() {
        when(subjectRepository.findByNormalizedName("direito constitucional"))
                .thenReturn(Optional.of(new Subject("Direito Constitucional", "direito constitucional")));

        assertThatThrownBy(() -> subjectService.create(actorId, "direito constitucional"))
                .isInstanceOf(ApiException.class)
                .satisfies(
                        ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.SUBJECT_NAME_TAKEN));
    }

    @Test
    void givenExistingSubject_whenRenaming_thenNameAndNormalizedNameAreUpdatedAndAuditRecorded() {
        Subject subject = new Subject("Direito Penal", "direito penal");
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(subjectRepository.findByNormalizedName("direito processual penal")).thenReturn(Optional.empty());
        when(subjectRepository.saveAndFlush(any(Subject.class))).thenAnswer(call -> call.getArgument(0));
        when(deckService.countActiveBySubject(subject.getId())).thenReturn(4L);

        SubjectWithDeckCount result = subjectService.update(
                actorId, subject.getId(), new UpdateSubjectCommand("Direito Processual Penal", null));

        assertThat(result.subject().getName()).isEqualTo("Direito Processual Penal");
        assertThat(result.deckCount()).isEqualTo(4L);
        verify(auditRecorder).recordRenamed(actorId, subject, "Direito Penal", "Direito Processual Penal");
    }

    @Test
    void givenRenameToAnotherSubjectsName_whenUpdating_thenThrowsSubjectNameTaken() {
        Subject subject = new Subject("Direito Penal", "direito penal");
        Subject other = new Subject("Direito Civil", "direito civil");
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(subjectRepository.findByNormalizedName("direito civil")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> subjectService.update(
                        actorId, subject.getId(), new UpdateSubjectCommand("Direito Civil", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(
                        ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.SUBJECT_NAME_TAKEN));
    }

    @Test
    void givenSameSubjectRenamedToSameName_whenUpdating_thenSucceedsAndDeactivationIsRecorded() {
        Subject subject = new Subject("Direito Penal", "direito penal");
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(subjectRepository.findByNormalizedName("direito penal")).thenReturn(Optional.of(subject));
        when(subjectRepository.saveAndFlush(any(Subject.class))).thenAnswer(call -> call.getArgument(0));
        when(deckService.countActiveBySubject(subject.getId())).thenReturn(0L);

        SubjectWithDeckCount result =
                subjectService.update(actorId, subject.getId(), new UpdateSubjectCommand("Direito Penal", false));

        assertThat(result.subject().isActive()).isFalse();
        verify(auditRecorder).recordActiveChanged(actorId, subject, true, false);
        verify(auditRecorder, never()).recordRenamed(any(), any(), any(), any());
    }

    @Test
    void givenUnknownSubject_whenUpdating_thenThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(subjectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subjectService.update(actorId, id, new UpdateSubjectCommand(null, true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenBlankName_whenCreating_thenThrowsValidationFailed() {
        assertThatThrownBy(() -> subjectService.create(actorId, "   "))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
