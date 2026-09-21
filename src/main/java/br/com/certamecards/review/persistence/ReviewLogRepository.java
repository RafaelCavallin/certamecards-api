package br.com.certamecards.review.persistence;

import br.com.certamecards.review.domain.ReviewLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {

    List<ReviewLog> findByCardIdOrderByReviewedAtAsc(UUID cardId);

    @Query("select count(rl) from ReviewLog rl where rl.cardId = :cardId and rl.userId = :userId "
            + "and rl.id not in (select rv.reviewId from ReviewVoid rv)")
    long countAcceptedByCardIdAndUserId(UUID cardId, UUID userId);
}
