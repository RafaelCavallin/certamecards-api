package br.com.certamecards.officialdeck.service;

import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialCardService {

    private final CardRepository cardRepository;
    private final OfficialDeckAccess deckAccess;
    private final OfficialDeckCardCountAdjuster countAdjuster;
    private final OfficialCardContentApplier contentApplier;
    private final OfficialCardContentChangeResolver contentChangeResolver;
    private final OfficialCardAuditRecorder auditRecorder;
    private final OfficialContentUpdateEnqueuer enqueuer;
    private final Clock clock;

    public OfficialCardService(
            CardRepository cardRepository,
            OfficialDeckAccess deckAccess,
            OfficialDeckCardCountAdjuster countAdjuster,
            OfficialCardContentApplier contentApplier,
            OfficialCardContentChangeResolver contentChangeResolver,
            OfficialCardAuditRecorder auditRecorder,
            OfficialContentUpdateEnqueuer enqueuer,
            Clock clock) {
        this.cardRepository = cardRepository;
        this.deckAccess = deckAccess;
        this.countAdjuster = countAdjuster;
        this.contentApplier = contentApplier;
        this.contentChangeResolver = contentChangeResolver;
        this.auditRecorder = auditRecorder;
        this.enqueuer = enqueuer;
        this.clock = clock;
    }

    @Transactional
    public OfficialCardUpdateResult update(UUID actorId, UUID cardId, UpdateOfficialCardCommand command) {
        Card card = findOfficialCard(cardId);
        ensureVersionMatches(card, command.expectedVersion());
        Deck deck = deckAccess.lockOfficial(card.getDeckId());
        OfficialDeckStatus status =
                OfficialDeckStatus.fromCode(deck.getOfficialMeta().getOfficialStatus());
        boolean contentChanged = contentChangeResolver.resolve(status, command);
        Map<String, AuditChange> changes = contentApplier.apply(card, command);
        Instant now = clock.instant();
        card.touch(now);
        Card savedCard = cardRepository.save(card);
        countAdjuster.touchAndSave(deck, now);
        if (contentChanged) {
            enqueuer.enqueue(savedCard, command.note(), now);
        }
        return recordUpdate(actorId, deck, savedCard, contentChanged, command.note(), changes);
    }

    private OfficialCardUpdateResult recordUpdate(
            UUID actorId, Deck deck, Card card, boolean contentChanged, String note, Map<String, AuditChange> changes) {
        int affectedSubscribers = contentChanged ? deck.getOfficialMeta().getSubscriberCount() : 0;
        if (contentChanged) {
            changes.put("note", new AuditChange(null, note));
            auditRecorder.recordContentChanged(actorId, card, cardLabel(deck, card), changes);
        } else {
            auditRecorder.recordUpdated(actorId, card, cardLabel(deck, card), changes);
        }
        return new OfficialCardUpdateResult(card, affectedSubscribers, contentChanged);
    }

    private Card findOfficialCard(UUID cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
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
