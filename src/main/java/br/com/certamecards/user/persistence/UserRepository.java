package br.com.certamecards.user.persistence;

import br.com.certamecards.user.domain.User;
import br.com.certamecards.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    long countByRole(UserRole role);

    List<User> findAllByRole(UserRole role);
}
