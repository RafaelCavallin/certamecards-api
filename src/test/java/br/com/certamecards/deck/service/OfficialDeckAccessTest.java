package br.com.certamecards.deck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.persistence.DeckRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialDeckAccessTest {

    private final DeckRepository deckRepository = mock(DeckRepository.class);
    private final OfficialDeckAccess access = new OfficialDeckAccess(deckRepository);

    @Test
    void givenOwnedDeck_whenFindingOfficial_thenThrowsNotFound() {
        Deck owned = new Deck(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Meu deck");
        when(deckRepository.findById(owned.getId())).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> access.findOfficial(owned.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void givenOfficialDeck_whenFindingOfficial_thenReturnsIt() {
        Deck official = new Deck(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "CF/88",
                br.com.certamecards.deck.domain.DeckOrigin.OFFICIAL_SUBSCRIPTION);
        when(deckRepository.findById(official.getId())).thenReturn(Optional.of(official));

        assertThat(access.findOfficial(official.getId())).isSameAs(official);
    }

    @Test
    void givenUnknownId_whenFindingById_thenReturnsNull() {
        UUID id = UUID.randomUUID();
        when(deckRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(access.findById(id)).isNull();
    }

    @Test
    void givenOwnedDeck_whenLockingOfficial_thenThrowsNotFound() {
        Deck owned = new Deck(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Meu deck");
        when(deckRepository.findByIdForUpdate(owned.getId())).thenReturn(Optional.of(owned));

        assertThatThrownBy(() -> access.lockOfficial(owned.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
