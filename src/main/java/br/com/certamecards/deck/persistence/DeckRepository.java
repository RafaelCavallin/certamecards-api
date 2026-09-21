package br.com.certamecards.deck.persistence;

import br.com.certamecards.deck.domain.Deck;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    Optional<Deck> findByIdAndOwnerId(UUID id, UUID ownerId);

    long countBySubjectIdAndAudit_DeletedAtIsNull(UUID subjectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deck d where d.id = :id and d.ownerId = :ownerId")
    Optional<Deck> findByIdAndOwnerIdForUpdate(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deck d where d.id = :id")
    Optional<Deck> findByIdForUpdate(UUID id);

    @Query(
            value =
                    """
                    SELECT d.* FROM decks d
                    WHERE d.id = :deckId AND (d.owner_id = :userId OR EXISTS (
                        SELECT 1 FROM deck_subscriptions s
                        WHERE s.deck_id = d.id AND s.user_id = :userId AND s.cancelled_at IS NULL))
                    """,
            nativeQuery = true)
    Optional<Deck> findAccessibleByIdAndUserId(UUID deckId, UUID userId);
}
