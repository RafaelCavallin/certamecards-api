package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.domain.DeckOrigin;
import br.com.certamecards.deck.service.OfficialDeckAccess;
import br.com.certamecards.support.MutableClock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialCardServiceTest {

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final OfficialDeckAccess deckAccess = mock(OfficialDeckAccess.class);
    private final OfficialDeckCardCountAdjuster countAdjuster = mock(OfficialDeckCardCountAdjuster.class);
    private final OfficialCardContentApplier contentApplier = mock(OfficialCardContentApplier.class);
    private final OfficialCardContentChangeResolver contentChangeResolver =
            mock(OfficialCardContentChangeResolver.class);
    private final OfficialCardAuditRecorder auditRecorder = mock(OfficialCardAuditRecorder.class);
    private final OfficialContentUpdateEnqueuer enqueuer = mock(OfficialContentUpdateEnqueuer.class);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-19T14:05:00Z"), ZoneOffset.UTC);
    private final OfficialCardService service = new OfficialCardService(
            cardRepository,
            deckAccess,
            countAdjuster,
            contentApplier,
            contentChangeResolver,
            auditRecorder,
            enqueuer,
            clock);
    private final UUID actorId = UUID.randomUUID();

    @Test
    void givenUnknownCard_whenUpdating_thenThrowsNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(actorId, cardId, command(0)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenMismatchedVersion_whenUpdating_thenThrowsVersionConflict() {
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "F", "B");
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.update(actorId, cardId, command(9)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    @Test
    void givenCorrectionWithoutContentChange_whenUpdating_thenAffectedSubscribersIsZero() {
        UUID cardId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Card card = new Card(cardId, deckId, "F", "B");
        Deck deck = publishedDeckWithSubscribers(deckId, 120);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);
        when(contentChangeResolver.resolve(any(), any())).thenReturn(false);
        when(contentApplier.apply(any(), any()))
                .thenReturn(new java.util.LinkedHashMap<>(Map.of("back", new AuditChange("B", "B2"))));
        when(cardRepository.save(card)).thenReturn(card);

        OfficialCardUpdateResult result = service.update(actorId, cardId, command(0));

        assertThat(result.affectedSubscribers()).isZero();
        assertThat(result.contentUpdateQueued()).isFalse();
        verify(enqueuer, never()).enqueue(any(), any(), any());
        verify(auditRecorder)
                .recordUpdated(actorId, card, deck.getName() + " · F", Map.of("back", new AuditChange("B", "B2")));
    }

    @Test
    void givenContentChange_whenUpdating_thenAffectedSubscribersMatchesDeckSubscriberCount() {
        UUID cardId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Card card = new Card(cardId, deckId, "F", "B");
        Deck deck = publishedDeckWithSubscribers(deckId, 120);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(deckAccess.lockOfficial(deckId)).thenReturn(deck);
        when(contentChangeResolver.resolve(any(), any())).thenReturn(true);
        when(contentApplier.apply(any(), any())).thenReturn(new java.util.LinkedHashMap<>());
        when(cardRepository.save(card)).thenReturn(card);

        OfficialCardUpdateResult result =
                service.update(actorId, cardId, new UpdateOfficialCardCommand(null, "B2", null, true, "Motivo", 0));

        assertThat(result.affectedSubscribers()).isEqualTo(120);
        assertThat(result.contentUpdateQueued()).isTrue();
        verify(enqueuer).enqueue(card, "Motivo", clock.instant());
        verify(auditRecorder)
                .recordContentChanged(
                        actorId, card, deck.getName() + " · F", Map.of("note", new AuditChange(null, "Motivo")));
    }

    private Deck publishedDeckWithSubscribers(UUID deckId, int subscriberCount) {
        Deck deck = new Deck(deckId, null, UUID.randomUUID(), "CF/88", DeckOrigin.OFFICIAL_SUBSCRIPTION);
        deck.getOfficialMeta().markDraft(clock.instant());
        deck.getOfficialMeta().changeStatus("published");
        try {
            var field = deck.getOfficialMeta().getClass().getDeclaredField("subscriberCount");
            field.setAccessible(true);
            field.setInt(deck.getOfficialMeta(), subscriberCount);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return deck;
    }

    private UpdateOfficialCardCommand command(int expectedVersion) {
        return new UpdateOfficialCardCommand(null, "B2", null, false, null, expectedVersion);
    }
}
