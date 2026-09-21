package br.com.certamecards.review.service;

import br.com.certamecards.card.service.CardAccessResolver;
import br.com.certamecards.review.domain.CardReviewHistory;
import br.com.certamecards.review.persistence.ReviewLogHistoryQuery;
import br.com.certamecards.review.persistence.ReviewVoidHistoryQuery;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ReviewHistoryService {

    private final CardAccessResolver cardAccessResolver;
    private final ReviewLogHistoryQuery reviewLogHistoryQuery;
    private final ReviewVoidHistoryQuery reviewVoidHistoryQuery;

    public ReviewHistoryService(
            CardAccessResolver cardAccessResolver,
            ReviewLogHistoryQuery reviewLogHistoryQuery,
            ReviewVoidHistoryQuery reviewVoidHistoryQuery) {
        this.cardAccessResolver = cardAccessResolver;
        this.reviewLogHistoryQuery = reviewLogHistoryQuery;
        this.reviewVoidHistoryQuery = reviewVoidHistoryQuery;
    }

    public CardReviewHistory historyForCard(UUID userId, UUID cardId) {
        cardAccessResolver.resolveForRead(userId, cardId);
        return new CardReviewHistory(
                reviewLogHistoryQuery.fetch(userId, cardId), reviewVoidHistoryQuery.fetch(userId, cardId));
    }
}
