package br.com.certamecards.review.service;

import br.com.certamecards.card.service.CardAccessResolver;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.review.domain.CardReviewHistory;
import br.com.certamecards.review.domain.ReviewHistoryCursor;
import br.com.certamecards.review.domain.ReviewLogEntry;
import br.com.certamecards.review.persistence.ReviewLogHistoryQuery;
import br.com.certamecards.review.persistence.ReviewVoidHistoryQuery;
import java.util.List;
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

    public CardReviewHistory historyForCard(UUID userId, UUID cardId, String opaqueCursor, int limit) {
        cardAccessResolver.resolveForRead(userId, cardId);
        ReviewHistoryCursor cursor = decodeCursor(opaqueCursor);
        List<ReviewLogEntry> fetched = reviewLogHistoryQuery.fetch(userId, cardId, cursor, limit + 1);
        boolean hasMore = fetched.size() > limit;
        List<ReviewLogEntry> page = hasMore ? fetched.subList(0, limit) : fetched;
        return new CardReviewHistory(page, reviewVoidHistoryQuery.fetch(userId, cardId), nextCursorOf(page), hasMore);
    }

    private String nextCursorOf(List<ReviewLogEntry> page) {
        if (page.isEmpty()) {
            return null;
        }
        ReviewLogEntry last = page.get(page.size() - 1);
        return new ReviewHistoryCursor(last.eventAt(), last.eventCounter(), last.eventDeviceId(), last.operationId())
                .encode();
    }

    private ReviewHistoryCursor decodeCursor(String opaqueCursor) {
        if (opaqueCursor == null || opaqueCursor.isBlank()) {
            return null;
        }
        try {
            return ReviewHistoryCursor.decode(opaqueCursor);
        } catch (RuntimeException ex) {
            throw ApiException.of(ErrorCode.VALIDATION_FAILED);
        }
    }
}
