package br.com.certamecards.card.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardTest {

    @Test
    void givenIdDeckFrontAndBack_whenConstructing_thenTypeDefaultsToBasic() {
        UUID id = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        Card card = new Card(id, deckId, "Prazo do MS?", "120 dias");
        assertThat(card.getId()).isEqualTo(id);
        assertThat(card.getDeckId()).isEqualTo(deckId);
        assertThat(card.getFront()).isEqualTo("Prazo do MS?");
        assertThat(card.getBack()).isEqualTo("120 dias");
        assertThat(card.getType()).isEqualTo(CardType.BASIC);
        assertThat(card.getSource()).isNull();
        assertThat(card.getVersion()).isEqualTo(0);
        assertThat(card.getChangeSeq()).isNull();
        assertThat(card.getAudit().getCreatedAt()).isNull();
        assertThat(card.isDeleted()).isFalse();
    }

    @Test
    void givenCard_whenEditingContent_thenGettersReflectChanges() {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "Prazo do MS?", "120 dias");
        card.editContent("Novo prazo?", "60 dias", "Lei 12.016/09");
        assertThat(card.getFront()).isEqualTo("Novo prazo?");
        assertThat(card.getBack()).isEqualTo("60 dias");
        assertThat(card.getSource()).isEqualTo("Lei 12.016/09");
    }

    @Test
    void givenCard_whenMarkingDeleted_thenIsDeletedIsTrue() {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "Prazo do MS?", "120 dias");
        card.markDeleted(Instant.parse("2026-09-17T12:00:00Z"));
        assertThat(card.isDeleted()).isTrue();
    }
}
