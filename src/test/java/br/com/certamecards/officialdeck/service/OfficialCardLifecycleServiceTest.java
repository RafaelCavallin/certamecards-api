package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.card.service.CardCreationResult;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.support.MutableClock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialCardLifecycleServiceTest {

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final OfficialDeckCardCountAdjuster countAdjuster = mock(OfficialDeckCardCountAdjuster.class);
    private final OfficialCardAuditRecorder auditRecorder = mock(OfficialCardAuditRecorder.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-19T14:00:00Z"), ZoneOffset.UTC);
    private final OfficialCardLifecycleService service =
            new OfficialCardLifecycleService(cardRepository, deckAccess, countAdjuster, auditRecorder, clock);
    private final UUID actorId = UUID.randomUUID();

    @Test
    void givenNewCardId_whenCreating_thenSavesCardAndIncrementsDeckCount() {
        UUID cardId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);
        when(cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId)).thenReturn(0L);
        when(cardRepository.save(any(Card.class))).thenAnswer(call -> call.getArgument(0));

        CardCreationResult result =
                service.create(actorId, new CreateOfficialCardCommand(cardId, deckId, "Frente", "Verso", null));

        assertThat(result.created()).isTrue();
        assertThat(result.card().getDeckId()).isEqualTo(deckId);
        org.mockito.Mockito.verify(countAdjuster).increment(deck, clock.instant());
        org.mockito.Mockito.verify(auditRecorder)
                .recordCreated(org.mockito.ArgumentMatchers.eq(actorId), any(Card.class), any());
    }

    @Test
    void givenDeckAtCardLimit_whenCreating_thenThrowsDeckCardLimit() {
        UUID cardId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);
        when(cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId))
                .thenReturn((long) CardLimits.MAX_CARDS_PER_DECK);

        assertThatThrownBy(() ->
                        service.create(actorId, new CreateOfficialCardCommand(cardId, deckId, "Frente", "Verso", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.DECK_CARD_LIMIT));
    }

    @Test
    void givenExistingCardId_whenCreating_thenIsIdempotent() {
        UUID cardId = UUID.randomUUID();
        Card existing = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existing));

        CardCreationResult result = service.create(
                actorId, new CreateOfficialCardCommand(cardId, UUID.randomUUID(), "Frente", "Verso", null));

        assertThat(result.created()).isFalse();
        assertThat(result.card()).isSameAs(existing);
    }

    @Test
    void givenMismatchedVersion_whenDeleting_thenThrowsVersionConflict() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.delete(actorId, cardId, 5))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    @Test
    void givenMatchingVersion_whenDeleting_thenMarksDeletedAndDecrementsDeckCount() {
        UUID cardId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Card card = new Card(cardId, deckId, "Frente", "Verso");
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);

        service.delete(actorId, cardId, 0);

        assertThat(card.isDeleted()).isTrue();
        org.mockito.Mockito.verify(countAdjuster).decrement(deck, clock.instant());
        org.mockito.Mockito.verify(auditRecorder)
                .recordDeleted(
                        org.mockito.ArgumentMatchers.eq(actorId), org.mockito.ArgumentMatchers.eq(cardId), any());
    }
}
