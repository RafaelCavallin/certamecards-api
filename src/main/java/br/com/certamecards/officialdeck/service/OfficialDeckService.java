package br.com.certamecards.officialdeck.service;

import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.DeckCreationResult;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.subject.service.SubjectLookup;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialDeckService {

    private final OfficialDeckAccess deckAccess;
    private final SubjectLookup subjectLookup;
    private final OfficialDeckUpdateApplier updateApplier;
    private final OfficialDeckAuditRecorder auditRecorder;
    private final Clock clock;

    public OfficialDeckService(
            OfficialDeckAccess deckAccess,
            SubjectLookup subjectLookup,
            OfficialDeckUpdateApplier updateApplier,
            OfficialDeckAuditRecorder auditRecorder,
            Clock clock) {
        this.deckAccess = deckAccess;
        this.subjectLookup = subjectLookup;
        this.updateApplier = updateApplier;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional
    public DeckCreationResult create(UUID actorId, CreateOfficialDeckCommand command) {
        Deck existing = deckAccess.findById(command.id());
        if (existing != null) {
            return new DeckCreationResult(existing, false);
        }
        subjectLookup.requireActive(command.subjectId());
        Deck saved = deckAccess.save(buildDraft(command));
        auditRecorder.recordCreated(actorId, saved);
        return new DeckCreationResult(saved, true);
    }

    public Deck findOfficial(UUID deckId) {
        return deckAccess.findOfficial(deckId);
    }

    @Transactional
    public Deck update(UUID actorId, UUID deckId, UpdateOfficialDeckCommand command) {
        Deck deck = deckAccess.lockOfficial(deckId);
        ensureVersionMatches(deck, command.expectedVersion());
        Map<String, AuditChange> changes = updateApplier.apply(deck, command);
        deck.updateSearchText(OfficialSearchText.build(deck.getName(), deck.getDescription()));
        touchContent(deck, clock.instant());
        Deck saved = deckAccess.save(deck);
        auditRecorder.recordUpdated(actorId, saved, changes);
        return saved;
    }

    private Deck buildDraft(CreateOfficialDeckCommand command) {
        Deck deck = new Deck(
                command.id(), null, command.subjectId(), command.name().strip(), DeckOrigin.OFFICIAL_SUBSCRIPTION);
        deck.getOfficialMeta().markDraft(clock.instant());
        deck.getAudit().initialize(clock.instant());
        deck.changeDescription(normalizedDescription(command.description()));
        deck.updateSearchText(OfficialSearchText.build(deck.getName(), deck.getDescription()));
        return deck;
    }

    private void touchContent(Deck deck, Instant now) {
        deck.getOfficialMeta().touchContentUpdatedAt(now);
        deck.touch(now);
    }

    private String normalizedDescription(String description) {
        return description == null || description.isBlank() ? null : description.strip();
    }

    private void ensureVersionMatches(Deck deck, int expectedVersion) {
        if (deck.getVersion() != expectedVersion) {
            throw ApiException.of(ErrorCode.VERSION_CONFLICT);
        }
    }
}
