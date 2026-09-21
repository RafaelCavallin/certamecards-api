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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialDeckStatusService {

    private final OfficialDeckAccess deckAccess;
    private final OfficialDeckStatusTransitions statusTransitions;
    private final OfficialDeckDeletionGuard deletionGuard;
    private final OfficialDeckAuditRecorder auditRecorder;
    private final Clock clock;

    public OfficialDeckStatusService(
            OfficialDeckAccess deckAccess,
            OfficialDeckStatusTransitions statusTransitions,
            OfficialDeckDeletionGuard deletionGuard,
            OfficialDeckAuditRecorder auditRecorder,
            Clock clock) {
        this.deckAccess = deckAccess;
        this.statusTransitions = statusTransitions;
        this.deletionGuard = deletionGuard;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public Deck changeStatus(UUID actorId, UUID deckId, OfficialDeckStatus target, int expectedVersion) {
        Deck deck = deckAccess.lockOfficial(deckId);
        ensureVersionMatches(deck, expectedVersion);
        OfficialDeckStatus current =
                OfficialDeckStatus.fromCode(deck.getOfficialMeta().getOfficialStatus());
        statusTransitions.validate(
                current,
                target,
                deck.getOfficialMeta().getCardCount(),
                deck.getOfficialMeta().getSubscriberCount());
        deck.getOfficialMeta().changeStatus(target.code());
        Deck saved = deckAccess.save(deck);
        auditRecorder.recordStatusChanged(actorId, saved, current, target);
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
    }

    private void ensureVersionMatches(Deck deck, int expectedVersion) {
        if (deck.getVersion() != expectedVersion) {
            throw ApiException.of(ErrorCode.VERSION_CONFLICT);
        }
    }
}
