package br.com.certamecards.officialdeck.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.officialdeck.persistence.DeckSubscriptionsQuery;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialDeckDeletionGuardTest {

    private final DeckSubscriptionsQuery subscriptionsQuery = mock(DeckSubscriptionsQuery.class);
    private final OfficialDeckDeletionGuard guard = new OfficialDeckDeletionGuard(subscriptionsQuery);

    @Test
    void givenDeckThatNeverHadSubscribers_whenEnsuringDeletable_thenSucceeds() {
        UUID deckId = UUID.randomUUID();
        when(subscriptionsQuery.everHadSubscribers(deckId)).thenReturn(false);

        assertThatCode(() -> guard.ensureDeletable(deckId)).doesNotThrowAnyException();
    }

    @Test
    void givenDeckThatEverHadSubscribers_whenEnsuringDeletable_thenThrowsEvenIfAllCancelled() {
        UUID deckId = UUID.randomUUID();
        when(subscriptionsQuery.everHadSubscribers(deckId)).thenReturn(true);

        assertThatThrownBy(() -> guard.ensureDeletable(deckId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.OFFICIAL_DECK_HAS_SUBSCRIBERS));
    }
}
