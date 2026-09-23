package br.com.certamecards.officialdeck.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.officialdeck.domain.OfficialDeckDeletionGuard;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatus;
import br.com.certamecards.officialdeck.domain.OfficialDeckStatusTransitions;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialDeckStatusService {

    private static final Logger log = LoggerFactory.getLogger(OfficialDeckStatusService.class);

    private final OfficialDeckAccess deckAccess;
    private final OfficialDeckStatusTransitions statusTransitions;
    private final OfficialDeckDeletionGuard deletionGuard;
    private final OfficialDeckAuditRecorder auditRecorder;
    private final Clock clock;
    private final OfficialMetrics metrics;

    public OfficialDeckStatusService(
            OfficialDeckAccess deckAccess,
            OfficialDeckStatusTransitions statusTransitions,
            OfficialDeckDeletionGuard deletionGuard,
            OfficialDeckAuditRecorder auditRecorder,
            Clock clock,
            OfficialMetrics metrics) {
        this.deckAccess = deckAccess;
        this.statusTransitions = statusTransitions;
        this.deletionGuard = deletionGuard;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Transactional
    public Deck changeStatus(UUID actorId, UUID deckId, OfficialDeckStatus target, int expectedVersion) {
        Deck deck = deckAccess.lockOfficial(deckId);
        ensureVersionMatches(deck, expectedVersion);
        OfficialDeckStatus current =
                OfficialDeckStatus.fromCode(deck.getOfficialMeta().getOfficialStatus());
        validate(deck, current, target);
        deck.getOfficialMeta().changeStatus(target.code());
        Deck saved = deckAccess.save(deck);
        auditRecorder.recordStatusChanged(actorId, saved, current, target);
        metrics.deckStatus(target.code(), "applied");
        log.info("Official deck {} moved from {} to {}", deckId, current.code(), target.code());
        return saved;
    }

    @Transactional
    public void delete(UUID actorId, UUID deckId, int expectedVersion) {
        Deck deck = deckAccess.lockOfficial(deckId);
        ensureVersionMatches(deck, expectedVersion);
        deletionGuard.ensureDeletable(deckId);
        deck.markDeleted(clock.instant());
        deckAccess.save(deck);
        auditRecorder.recordDeleted(actorId, deckId, deck.getName());
        log.info("Official deck {} deleted", deckId);
    }

    private void validate(Deck deck, OfficialDeckStatus current, OfficialDeckStatus target) {
        try {
            statusTransitions.validate(
                    current,
                    target,
                    deck.getOfficialMeta().getCardCount(),
                    deck.getOfficialMeta().getSubscriberCount());
        } catch (ApiException exception) {
            metrics.deckStatus(target.code(), "rejected");
            throw exception;
        }
    }

    private void ensureVersionMatches(Deck deck, int expectedVersion) {
        if (deck.getVersion() != expectedVersion) {
            throw ApiException.of(ErrorCode.VERSION_CONFLICT);
        }
    }
}
