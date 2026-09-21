package br.com.certamecards.subject.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.subject.domain.Subject;
import br.com.certamecards.subject.domain.SubjectNameNormalizer;
import br.com.certamecards.subject.persistence.SubjectRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubjectLookup {

    private final SubjectRepository subjectRepository;

    public SubjectLookup(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public Subject resolveActiveByNameOrFirst(String name) {
        if (name == null) {
            return subjectRepository.findFirstByActiveTrue().orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        }
        String normalizedName = SubjectNameNormalizer.normalize(name);
        return subjectRepository
                .findByNormalizedName(normalizedName)
                .filter(Subject::isActive)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    public Subject requireActive(UUID id) {
        Subject subject = subjectRepository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
        if (!subject.isActive()) {
            throw ApiException.of(ErrorCode.SUBJECT_INACTIVE);
        }
        return subject;
    }
}
