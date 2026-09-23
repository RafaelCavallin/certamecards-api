package br.com.certamecards.sync.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SyncOperationKindTest {

    @Test
    void givenKnownValue_whenParsing_thenReturnsMatchingKind() {
        assertThat(SyncOperationKind.from("card_delete")).isEqualTo(SyncOperationKind.CARD_DELETE);
    }

    @Test
    void givenUnknownValue_whenParsing_thenThrows() {
        assertThatThrownBy(() -> SyncOperationKind.from("unknown_kind")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenKind_whenReadingValue_thenReturnsSnakeCase() {
        assertThat(SyncOperationKind.DECK_RESET.value()).isEqualTo("deck_reset");
    }
}
