package br.com.certamecards.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.domain.CardLimits;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.service.DeckService;
import br.com.certamecards.review.service.CardStateService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final DeckService deckService = mock(DeckService.class);
    private final CardStateService cardStateService = mock(CardStateService.class);
    private final CardAccessResolver cardAccessResolver = mock(CardAccessResolver.class);
    private final CardLimitGuard limitGuard = new CardLimitGuard(cardRepository);
    private final CardRestorer restorer = new CardRestorer(cardRepository, deckService, limitGuard, FIXED_CLOCK);
    private final CardService cardService = new CardService(
            cardRepository, deckService, cardStateService, cardAccessResolver, limitGuard, restorer, FIXED_CLOCK);

    @Test
    @DisplayName("TU-30 — 5.000 cartões no deck lança deck_card_limit")
    void givenDeckAtCardLimit_whenCreating_thenThrowsDeckCardLimit() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, UUID.randomUUID(), "CF/88");
        when(cardRepository.findById(any())).thenReturn(Optional.empty());
        when(deckService.lockOwned(ownerId, deckId)).thenReturn(deck);
        when(cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId))
                .thenReturn((long) CardLimits.MAX_CARDS_PER_DECK);
        CreateCardCommand command =
                new CreateCardCommand(UUID.randomUUID(), deckId, ownerId, new CardContent("Frente", "Verso", null));

        assertThatThrownBy(() -> cardService.create(command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.DECK_CARD_LIMIT));
    }

    @Test
    @DisplayName("TU-30 — 50.000 cartões do usuário lança user_card_limit")
    void givenUserAtCardLimit_whenCreating_thenThrowsUserCardLimit() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, UUID.randomUUID(), "CF/88");
        when(cardRepository.findById(any())).thenReturn(Optional.empty());
        when(deckService.lockOwned(ownerId, deckId)).thenReturn(deck);
        when(cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId)).thenReturn(1L);
        when(cardRepository.countActiveByOwnerId(ownerId)).thenReturn((long) CardLimits.MAX_CARDS_PER_USER);
        CreateCardCommand command =
                new CreateCardCommand(UUID.randomUUID(), deckId, ownerId, new CardContent("Frente", "Verso", null));

        assertThatThrownBy(() -> cardService.create(command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_CARD_LIMIT));
    }

    @Test
    void givenCardWithinLimits_whenCreating_thenSavesTrimmedContent() {
        UUID ownerId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Deck deck = new Deck(deckId, ownerId, UUID.randomUUID(), "CF/88");
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());
        when(deckService.lockOwned(ownerId, deckId)).thenReturn(deck);
        when(cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(deckId)).thenReturn(0L);
        when(cardRepository.countActiveByOwnerId(ownerId)).thenReturn(0L);
        when(cardRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        CreateCardCommand command =
                new CreateCardCommand(cardId, deckId, ownerId, new CardContent("  Frente  ", "  Verso  ", null));

        CardCreationResult result = cardService.create(command);

        assertThat(result.created()).isTrue();
        assertThat(result.card().getFront()).isEqualTo("Frente");
        assertThat(result.card().getBack()).isEqualTo("Verso");
    }

    @Test
    void givenMismatchedVersion_whenUpdating_thenThrowsVersionConflict() {
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));
        UpdateCardCommand command = new UpdateCardCommand(new CardContent("Novo", "Novo verso", null), 9);

        assertThatThrownBy(() -> cardService.update(ownerId, cardId, command))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    @Test
    @DisplayName(
            "CA-24 — suspender cartão já excluído (ex.: oficial removido pelo admin) é not_found e não grava estado")
    void givenDeletedCard_whenSettingSuspension_thenThrowsNotFoundWithoutTouchingState() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        card.markDeleted(FIXED_NOW.minusSeconds(60));
        when(cardAccessResolver.resolveForRead(userId, cardId)).thenReturn(card);

        assertThatThrownBy(() -> cardService.setSuspension(userId, cardId, true))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(cardStateService, never()).setSuspended(any(), any(), anyBoolean(), any());
    }

    @Test
    void givenTargetDeckActive_whenRestoringCard_thenMovesAndClearsDeletion() {
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID targetDeckId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente antiga", "Verso antigo");
        card.markDeleted(FIXED_NOW.minusSeconds(60));
        Deck targetDeck = new Deck(targetDeckId, ownerId, UUID.randomUUID(), "Destino");
        when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));
        when(deckService.lockOwned(ownerId, targetDeckId)).thenReturn(targetDeck);
        when(cardRepository.countByDeckIdAndAudit_DeletedAtIsNull(targetDeckId)).thenReturn(0L);
        when(cardRepository.countActiveByOwnerId(ownerId)).thenReturn(0L);
        when(cardRepository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        Card restored = cardService.restore(ownerId, cardId, targetDeckId, new CardContent("Nova", "Nova verso", null));

        assertThat(restored.isDeleted()).isFalse();
        assertThat(restored.getDeckId()).isEqualTo(targetDeckId);
        assertThat(restored.getFront()).isEqualTo("Nova");
    }

    @Test
    void givenTargetDeckDeleted_whenRestoringCard_thenThrowsNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID targetDeckId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        Deck targetDeck = new Deck(targetDeckId, ownerId, UUID.randomUUID(), "Destino");
        targetDeck.markDeleted(FIXED_NOW);
        when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));
        when(deckService.lockOwned(ownerId, targetDeckId)).thenReturn(targetDeck);

        assertThatThrownBy(() ->
                        cardService.restore(ownerId, cardId, targetDeckId, new CardContent("Nova", "Verso", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenOwnedCard_whenDeleting_thenMarksDeleted() {
        UUID ownerId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        when(cardRepository.findByIdAndOwnerId(cardId, ownerId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        cardService.delete(ownerId, cardId, 0);

        assertThat(card.isDeleted()).isTrue();
    }
}
