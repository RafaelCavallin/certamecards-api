package br.com.certamecards.library.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.library.domain.DeckSubscription;
import br.com.certamecards.library.persistence.DeckSubscriptionStore;
import br.com.certamecards.review.domain.CardState;
import br.com.certamecards.review.persistence.CardStateRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class DeckContentService {

    private static final UUID FIRST_PAGE_CURSOR = new UUID(0L, 0L);

    private final OfficialDeckAccess deckAccess;
    private final DeckSubscriptionStore subscriptionStore;
    private final CardRepository cardRepository;
    private final CardStateRepository cardStateRepository;

    public DeckContentService(
            OfficialDeckAccess deckAccess,
            DeckSubscriptionStore subscriptionStore,
            CardRepository cardRepository,
            CardStateRepository cardStateRepository) {
        this.deckAccess = deckAccess;
        this.subscriptionStore = subscriptionStore;
        this.cardRepository = cardRepository;
        this.cardStateRepository = cardStateRepository;
    }

    public DeckContentPage content(UUID userId, UUID deckId, ContentCursor cursor) {
        deckAccess.findOfficial(deckId);
        if (subscriptionStore
                .find(userId, deckId)
                .filter(DeckSubscription::active)
                .isEmpty()) {
            throw ApiException.of(ErrorCode.NOT_SUBSCRIBED);
        }
        List<Card> fetched = fetchPage(deckId, cursor);
        boolean hasMore = fetched.size() > cursor.limit();
        List<Card> cards = hasMore ? fetched.subList(0, cursor.limit()) : fetched;
        UUID nextAfter = hasMore ? cards.getLast().getId() : null;
        return new DeckContentPage(cards, statesOf(userId, cards), nextAfter, hasMore);
    }

    private List<Card> fetchPage(UUID deckId, ContentCursor cursor) {
        UUID after = cursor.after() == null ? FIRST_PAGE_CURSOR : cursor.after();
        return cardRepository.findByDeckIdAndAudit_DeletedAtIsNullAndIdGreaterThanOrderById(
                deckId, after, PageRequest.ofSize(cursor.limit() + 1));
    }

    private List<CardState> statesOf(UUID userId, List<Card> cards) {
        List<UUID> cardIds = cards.stream().map(Card::getId).toList();
        return cardStateRepository.findByIdUserIdAndIdCardIdIn(userId, cardIds);
    }
}
