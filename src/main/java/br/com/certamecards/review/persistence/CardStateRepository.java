package br.com.certamecards.review.persistence;

import br.com.certamecards.review.domain.CardState;
import br.com.certamecards.review.domain.CardStateId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardStateRepository extends JpaRepository<CardState, CardStateId> {

    List<CardState> findByIdUserId(UUID userId);

    Optional<CardState> findByIdUserIdAndIdCardId(UUID userId, UUID cardId);

    List<CardState> findByIdUserIdAndIdCardIdIn(UUID userId, List<UUID> cardIds);
}
