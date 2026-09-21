package br.com.certamecards.subject.persistence;

import br.com.certamecards.subject.domain.Subject;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    Optional<Subject> findByNormalizedName(String normalizedName);

    Optional<Subject> findFirstByActiveTrue();
}
