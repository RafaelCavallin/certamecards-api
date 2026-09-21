package br.com.certamecards.card.persistence;

import br.com.certamecards.card.domain.Card;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CardRepository extends JpaRepository<Card, UUID> {

    long countByDeckIdAndAudit_DeletedAtIsNull(UUID deckId);

    Page<Card> findByDeckIdAndAudit_DeletedAtIsNullOrderById(UUID deckId, Pageable pageable);

    List<Card> findByDeckIdAndAudit_DeletedAtIsNullAndIdGreaterThanOrderById(
            UUID deckId, UUID after, Pageable pageable);

    @Query("select c from Card c, Deck d where c.id = :id and c.deckId = d.id and d.ownerId = :ownerId")
    Optional<Card> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("select c from Card c, Deck d where c.id in :ids and c.deckId = d.id and d.ownerId = :ownerId")
    List<Card> findAllByIdInAndOwnerId(List<UUID> ids, UUID ownerId);

    @Query("select count(c) from Card c, Deck d where c.deckId = d.id and d.ownerId = :ownerId "
            + "and c.audit.deletedAt is null")
    long countActiveByOwnerId(UUID ownerId);

    @Query(
            value =
                    """
                    SELECT c.* FROM cards c JOIN decks d ON d.id = c.deck_id
                    WHERE c.id = :cardId AND (d.owner_id = :userId OR EXISTS (
                        SELECT 1 FROM deck_subscriptions s
                        WHERE s.deck_id = d.id AND s.user_id = :userId AND s.cancelled_at IS NULL))
                    """,
            nativeQuery = true)
    Optional<Card> findAccessibleByIdAndUserId(UUID cardId, UUID userId);

    @Query(
            value =
                    """
                    SELECT c.* FROM cards c JOIN decks d ON d.id = c.deck_id
                    WHERE c.id IN (:cardIds) AND (d.owner_id = :userId OR EXISTS (
                        SELECT 1 FROM deck_subscriptions s
                        WHERE s.deck_id = d.id AND s.user_id = :userId AND s.cancelled_at IS NULL))
                    """,
            nativeQuery = true)
    List<Card> findAllAccessibleByIdInAndUserId(List<UUID> cardIds, UUID userId);
}
