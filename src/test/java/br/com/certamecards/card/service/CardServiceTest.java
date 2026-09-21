package br.com.certamecards.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.junit.jupiter.api.Test;

class CardServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final DeckService deckService = mock(DeckService.class);
    private final CardStateService cardStateService = mock(CardStateService.class);
    private final CardAccessResolver cardAccessResolver = mock(CardAccessResolver.class);
    private final CardService cardService =
            new CardService(cardRepository, deckService, cardStateService, cardAccessResolver, FIXED_CLOCK);

    @Test
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
        when(cardRepository.save(any())).thenAnswer(call -> call.getArgument(0));
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
