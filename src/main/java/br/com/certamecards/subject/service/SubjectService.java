package br.com.certamecards.subject.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.subject.domain.Subject;
import br.com.certamecards.subject.domain.SubjectNameNormalizer;
import br.com.certamecards.subject.persistence.SubjectRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final DeckService deckService;
    private final SubjectAuditRecorder auditRecorder;

    public SubjectService(
            SubjectRepository subjectRepository, DeckService deckService, SubjectAuditRecorder auditRecorder) {
        this.subjectRepository = subjectRepository;
        this.deckService = deckService;
        this.auditRecorder = auditRecorder;
    }

    public List<SubjectWithDeckCount> listWithDeckCount() {
        return subjectRepository.findAll().stream().map(this::withDeckCount).toList();
    }

    @Transactional
    public SubjectWithDeckCount create(UUID actorId, String rawName) {
        String name = requireName(rawName);
        String normalizedName = SubjectNameNormalizer.normalize(name);
        ensureNameFree(normalizedName, null);
        Subject saved = save(new Subject(name, normalizedName));
        auditRecorder.recordCreated(actorId, saved);
        return new SubjectWithDeckCount(saved, 0);
    }

    @Transactional
    public SubjectWithDeckCount update(UUID actorId, UUID id, UpdateSubjectCommand command) {
        Subject subject = subjectRepository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        applyRename(actorId, subject, command.name());
        applyActive(actorId, subject, command.active());
        return withDeckCount(save(subject));
    }

    private void applyRename(UUID actorId, Subject subject, String rawName) {
        if (rawName == null) {
            return;
        }
        String name = requireName(rawName);
        String normalizedName = SubjectNameNormalizer.normalize(name);
        ensureNameFree(normalizedName, subject.getId());
        String before = subject.getName();
        subject.rename(name, normalizedName);
        if (!before.equals(name)) {
            auditRecorder.recordRenamed(actorId, subject, before, name);
        }
    }

    private void applyActive(UUID actorId, Subject subject, Boolean active) {
        if (active == null) {
            return;
        }
        boolean before = subject.isActive();
        subject.setActive(active);
        if (before != active) {
            auditRecorder.recordActiveChanged(actorId, subject, before, active);
        }
    }

    private String requireName(String rawName) {
        String name = rawName.strip();
        if (name.isEmpty()) {
            throw ApiException.withDetail(ErrorCode.VALIDATION_FAILED, "Informe o nome da matéria.");
        }
        return name;
    }

    private void ensureNameFree(String normalizedName, UUID excludingId) {
        subjectRepository
                .findByNormalizedName(normalizedName)
                .filter(existing -> !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw ApiException.of(ErrorCode.SUBJECT_NAME_TAKEN);
                });
    }

    private Subject save(Subject subject) {
        try {
            return subjectRepository.saveAndFlush(subject);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.of(ErrorCode.SUBJECT_NAME_TAKEN);
        }
    }

    private SubjectWithDeckCount withDeckCount(Subject subject) {
        return new SubjectWithDeckCount(subject, deckService.countActiveBySubject(subject.getId()));
    }
}
