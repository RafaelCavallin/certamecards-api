package br.com.certamecards.review.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardStateIdTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    @Test
    void givenSameInstance_whenComparing_thenEqual() {
        CardStateId id = new CardStateId(userId, cardId);
        assertThat(id).isEqualTo(id);
    }

    @Test
    void givenEqualValues_whenComparing_thenEqualAndSameHashCode() {
        CardStateId first = new CardStateId(userId, cardId);
        CardStateId second = new CardStateId(userId, cardId);
        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void givenDifferentCardId_whenComparing_thenNotEqual() {
        CardStateId first = new CardStateId(userId, cardId);
        CardStateId second = new CardStateId(userId, UUID.randomUUID());
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void givenDifferentType_whenComparing_thenNotEqual() {
        CardStateId id = new CardStateId(userId, cardId);
        assertThat(id).isNotEqualTo("not-a-card-state-id");
    }

    @Test
    void givenUserIdAndCardId_whenConstructing_thenGettersReturnValues() {
        CardStateId id = new CardStateId(userId, cardId);
        assertThat(id.getUserId()).isEqualTo(userId);
        assertThat(id.getCardId()).isEqualTo(cardId);
    }
}
