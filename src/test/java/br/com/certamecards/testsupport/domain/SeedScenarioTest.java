package br.com.certamecards.testsupport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

class SeedScenarioTest {

    @Test
    void givenKnownCodes_whenParsing_thenReturnsMatchingScenario() {
        assertThat(SeedScenario.fromCode("due_cards")).isEqualTo(SeedScenario.DUE_CARDS);
        assertThat(SeedScenario.fromCode("leech_card")).isEqualTo(SeedScenario.LEECH_CARD);
        assertThat(SeedScenario.fromCode("large_deck")).isEqualTo(SeedScenario.LARGE_DECK);
        assertThat(SeedScenario.fromCode("official_deck")).isEqualTo(SeedScenario.OFFICIAL_DECK);
        assertThat(SeedScenario.fromCode("official_deck_with_subscribers"))
                .isEqualTo(SeedScenario.OFFICIAL_DECK_WITH_SUBSCRIBERS);
    }

    @Test
    void givenOfficialScenarios_whenAsking_thenOnlyThoseAreOfficialAndOnlyOneHasSubscribers() {
        assertThat(SeedScenario.OFFICIAL_DECK.official()).isTrue();
        assertThat(SeedScenario.OFFICIAL_DECK.withSubscribers()).isFalse();
        assertThat(SeedScenario.OFFICIAL_DECK_WITH_SUBSCRIBERS.official()).isTrue();
        assertThat(SeedScenario.OFFICIAL_DECK_WITH_SUBSCRIBERS.withSubscribers())
                .isTrue();
        assertThat(SeedScenario.DUE_CARDS.official()).isFalse();
    }

    @Test
    void givenUnknownCode_whenParsing_thenThrowsValidationFailed() {
        assertThatThrownBy(() -> SeedScenario.fromCode("unknown"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
    }
}
