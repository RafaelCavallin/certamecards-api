package br.com.certamecards.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardAccessResolverTest {

    private final CardRepository cardRepository = mock(CardRepository.class);
    private final CardAccessResolver resolver = new CardAccessResolver(cardRepository);

    @Test
    void givenOwnCard_whenResolvingForWrite_thenReturnsCard() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        when(cardRepository.findByIdAndOwnerId(cardId, userId)).thenReturn(Optional.of(card));

        assertThat(resolver.resolveForWrite(userId, cardId)).isSameAs(card);
    }

    @Test
    void givenSubscribedOfficialCard_whenResolvingForWrite_thenThrowsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findByIdAndOwnerId(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveForWrite(userId, cardId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenSubscribedOfficialCard_whenResolvingForRead_thenReturnsCard() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        when(cardRepository.findAccessibleByIdAndUserId(cardId, userId)).thenReturn(Optional.of(card));

        assertThat(resolver.resolveForRead(userId, cardId)).isSameAs(card);
    }

    @Test
    void givenUnsubscribedOfficialCardOrAnotherUsersCard_whenResolvingForRead_thenThrowsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findAccessibleByIdAndUserId(cardId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveForRead(userId, cardId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenEmptyCardIds_whenResolvingAllForRead_thenSkipsRepositoryCall() {
        UUID userId = UUID.randomUUID();

        Map<UUID, Card> result = resolver.resolveAllForRead(userId, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void givenAccessibleCardIds_whenResolvingAllForRead_thenIndexesByCardId() {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Card card = new Card(cardId, UUID.randomUUID(), "Frente", "Verso");
        when(cardRepository.findAllAccessibleByIdInAndUserId(List.of(cardId), userId))
                .thenReturn(List.of(card));

        Map<UUID, Card> result = resolver.resolveAllForRead(userId, List.of(cardId));

        assertThat(result).containsExactly(Map.entry(cardId, card));
    }
}
