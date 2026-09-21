package br.com.certamecards.card.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.review.domain.CardState;
import br.com.certamecards.review.service.CardStateService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final DeckService deckService;
    private final CardStateService cardStateService;
    private final CardAccessResolver cardAccessResolver;
    private final Clock clock;

    public CardService(
            CardRepository cardRepository,
            DeckService deckService,
            CardStateService cardStateService,
            CardAccessResolver cardAccessResolver,
            Clock clock) {
        this.cardRepository = cardRepository;
        this.deckService = deckService;
        this.cardStateService = cardStateService;
        this.cardAccessResolver = cardAccessResolver;
        this.clock = clock;
    }

    @Transactional
    public CardCreationResult create(CreateCardCommand command) {
        Card existing = cardRepository.findById(command.id()).orElse(null);
        if (existing != null) {
            return new CardCreationResult(existing, false);
        }
        Deck deck = deckService.lockOwned(command.ownerId(), command.deckId());
        ensureWithinLimits(deck.getId(), command.ownerId());
        Card card = buildCard(command, deck.getId());
        return new CardCreationResult(cardRepository.save(card), true);
    }

    @Transactional
    public Card update(UUID ownerId, UUID cardId, UpdateCardCommand command) {
        Card card = findOwned(ownerId, cardId);
        ensureVersionMatches(card, command.expectedVersion());
        CardContent content = command.content();
        card.editContent(content.front().strip(), content.back().strip(), content.normalizedSource());
        card.touch(clock.instant());
        return cardRepository.save(card);
    }

    @Transactional
    public void delete(UUID ownerId, UUID cardId, int expectedVersion) {
        Card card = findOwned(ownerId, cardId);
        ensureVersionMatches(card, expectedVersion);
        card.markDeleted(clock.instant());
        cardRepository.save(card);
    }

    public Card findOwned(UUID ownerId, UUID cardId) {
        return cardRepository
                .findByIdAndOwnerId(cardId, ownerId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public CardState setSuspension(UUID userId, UUID cardId, boolean suspended) {
        cardAccessResolver.resolveForRead(userId, cardId);
        return cardStateService.setSuspended(userId, cardId, suspended, clock.instant());
    }

    private void ensureWithinLimits(UUID deckId, UUID ownerId) {
        if (cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId) >= CardLimits.MAX_CARDS_PER_DECK) {
            throw ApiException.of(ErrorCode.DECK_CARD_LIMIT);
        }
        if (cardRepository.countActiveByOwnerId(ownerId) >= CardLimits.MAX_CARDS_PER_USER) {
            throw ApiException.of(ErrorCode.USER_CARD_LIMIT);
        }
    }

    private Card buildCard(CreateCardCommand command, UUID deckId) {
        CardContent content = command.content();
        Card card = new Card(
                command.id(), deckId, content.front().strip(), content.back().strip());
        card.editContent(card.getFront(), card.getBack(), content.normalizedSource());
        card.getAudit().initialize(clock.instant());
        return card;
    }

    private void ensureVersionMatches(Card card, int expectedVersion) {
        if (card.getVersion() != expectedVersion) {
            throw ApiException.of(ErrorCode.VERSION_CONFLICT);
        }
    }
}
