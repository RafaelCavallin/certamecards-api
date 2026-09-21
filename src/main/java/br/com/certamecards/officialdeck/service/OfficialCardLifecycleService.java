package br.com.certamecards.officialdeck.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.card.service.CardCreationResult;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialCardLifecycleService {

    private final CardRepository cardRepository;
    private final OfficialDeckAccess deckAccess;
    private final OfficialDeckCardCountAdjuster countAdjuster;
    private final OfficialCardAuditRecorder auditRecorder;
    private final Clock clock;

    public OfficialCardLifecycleService(
            CardRepository cardRepository,
            OfficialDeckAccess deckAccess,
            OfficialDeckCardCountAdjuster countAdjuster,
            OfficialCardAuditRecorder auditRecorder,
            Clock clock) {
        this.cardRepository = cardRepository;
        this.deckAccess = deckAccess;
        this.countAdjuster = countAdjuster;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public CardCreationResult create(UUID actorId, CreateOfficialCardCommand command) {
        Card existing = cardRepository.findById(command.id()).orElse(null);
        if (existing != null) {
            return new CardCreationResult(existing, false);
        }
        Deck deck = deckAccess.lockOfficial(command.deckId());
        ensureWithinDeckLimit(deck.getId());
        Card saved = cardRepository.save(buildCard(command, deck.getId()));
        countAdjuster.increment(deck, clock.instant());
        auditRecorder.recordCreated(actorId, saved, cardLabel(deck, saved));
        return new CardCreationResult(saved, true);
    }

    @Transactional
    public void delete(UUID actorId, UUID cardId, int expectedVersion) {
        Card card = findOfficialCard(cardId);
        ensureVersionMatches(card, expectedVersion);
        Deck deck = deckAccess.lockOfficial(card.getDeckId());
        card.markDeleted(clock.instant());
        cardRepository.save(card);
        countAdjuster.decrement(deck, clock.instant());
        auditRecorder.recordDeleted(actorId, cardId, cardLabel(deck, card));
    }

    private Card findOfficialCard(UUID cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    private Card buildCard(CreateOfficialCardCommand command, UUID deckId) {
        Card card = new Card(
                command.id(), deckId, command.front().strip(), command.back().strip());
        card.editContent(card.getFront(), card.getBack(), normalizedSource(command.source()));
        card.getAudit().initialize(clock.instant());
        return card;
    }

    private void ensureWithinDeckLimit(UUID deckId) {
        if (cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId) >= CardLimits.MAX_CARDS_PER_DECK) {
            throw ApiException.of(ErrorCode.DECK_CARD_LIMIT);
        }
    }

    private String normalizedSource(String source) {
        return source == null || source.isBlank() ? null : source.strip();
    }

    private String cardLabel(Deck deck, Card card) {
        return deck.getName() + " · " + card.getFront();
    }

    private void ensureVersionMatches(Card card, int expectedVersion) {
        if (card.getVersion() != expectedVersion) {
            throw ApiException.of(ErrorCode.VERSION_CONFLICT);
        }
    }
}
