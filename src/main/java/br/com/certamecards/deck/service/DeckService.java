package br.com.certamecards.deck.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.persistence.DeckCardsQuery;
import br.com.certamecards.deck.persistence.DeckRepository;
import br.com.certamecards.subject.service.SubjectLookup;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final SubjectLookup subjectLookup;
    private final DeckCardsQuery deckCardsQuery;
    private final DeckUpdateApplier updateApplier;
    private final Clock clock;

    public DeckService(
            DeckRepository deckRepository,
            SubjectLookup subjectLookup,
            DeckCardsQuery deckCardsQuery,
            DeckUpdateApplier updateApplier,
            Clock clock) {
        this.deckRepository = deckRepository;
        this.subjectLookup = subjectLookup;
        this.deckCardsQuery = deckCardsQuery;
        this.updateApplier = updateApplier;
        this.clock = clock;
    }

    public long countActiveBySubject(UUID subjectId) {
        return deckRepository.countBySubjectIdAndAudit_DeletedAtIsNull(subjectId);
    }

    @Transactional
    public DeckCreationResult create(CreateDeckCommand command) {
        Deck existing = deckRepository.findById(command.id()).orElse(null);
        if (existing != null) {
            return new DeckCreationResult(existing, false);
        }
        subjectLookup.requireActive(command.subjectId());
        Deck deck = new Deck(
                command.id(),
                command.ownerId(),
                command.subjectId(),
                command.content().name().strip());
        deck.changeDescription(command.content().normalizedDescription());
        deck.getAudit().initialize(clock.instant());
        return new DeckCreationResult(deckRepository.saveAndFlush(deck), true);
    }

    @Transactional
    public Deck update(UUID ownerId, UUID deckId, UpdateDeckCommand command) {
        Deck deck = findOwned(ownerId, deckId);
        ensureVersionMatches(deck, command.expectedVersion());
        updateApplier.apply(deck, command);
        deck.touch(clock.instant());
        return deckRepository.saveAndFlush(deck);
    }

    @Transactional
    public Deck restore(UUID ownerId, UUID deckId, UpdateDeckCommand command) {
        Deck deck = findOwned(ownerId, deckId);
        updateApplier.apply(deck, command);
        deck.getAudit().restore(clock.instant());
        return deckRepository.saveAndFlush(deck);
    }

    @Transactional
    public void delete(UUID ownerId, UUID deckId, int expectedVersion) {
        Deck deck = findOwned(ownerId, deckId);
        ensureVersionMatches(deck, expectedVersion);
        deck.markDeleted(clock.instant());
        deckRepository.save(deck);
        deckCardsQuery.markAllDeleted(deckId, clock.instant());
    }

    public Deck lockOwned(UUID ownerId, UUID deckId) {
        return deckRepository
                .findByIdAndOwnerIdForUpdate(deckId, ownerId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    public Deck findOwned(UUID ownerId, UUID deckId) {
        return deckRepository
                .findByIdAndOwnerId(deckId, ownerId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    public Deck findAccessible(UUID userId, UUID deckId) {
        return deckRepository
                .findAccessibleByIdAndUserId(deckId, userId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    private void ensureVersionMatches(Deck deck, int expectedVersion) {
        if (deck.getVersion() != expectedVersion) {
            throw ApiException.of(ErrorCode.VERSION_CONFLICT);
        }
    }
}
