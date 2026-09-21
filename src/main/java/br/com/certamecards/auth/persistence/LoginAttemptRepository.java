package br.com.certamecards.auth.persistence;

import br.com.certamecards.auth.domain.LoginAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    List<LoginAttempt> findByEmailAndSuccessFalseOrderByAttemptedAtDesc(String email, Pageable pageable);
}
