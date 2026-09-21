package br.com.certamecards.deck.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeckTest {

    @Test
    void givenIdOwnerSubjectAndName_whenConstructing_thenOriginDefaultsToOwn() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        Deck deck = new Deck(id, ownerId, subjectId, "CF/88");
        assertThat(deck.getId()).isEqualTo(id);
        assertThat(deck.getOwnerId()).isEqualTo(ownerId);
        assertThat(deck.getSubjectId()).isEqualTo(subjectId);
        assertThat(deck.getName()).isEqualTo("CF/88");
        assertThat(deck.getOrigin()).isEqualTo(DeckOrigin.OWN);
        assertThat(deck.getDescription()).isNull();
        assertThat(deck.getOriginRef()).isNull();
        assertThat(deck.getVersion()).isEqualTo(0);
        assertThat(deck.getChangeSeq()).isNull();
        assertThat(deck.getAudit().getCreatedAt()).isNull();
        assertThat(deck.isDeleted()).isFalse();
    }

    @Test
    void givenDeck_whenRenamingChangingDescriptionAndSubject_thenGettersReflectChanges() {
        Deck deck = new Deck(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "CF/88");
        UUID newSubjectId = UUID.randomUUID();

        deck.rename("CF/88 atualizada");
        deck.changeDescription("Nova descrição");
        deck.changeSubject(newSubjectId);

        assertThat(deck.getName()).isEqualTo("CF/88 atualizada");
        assertThat(deck.getDescription()).isEqualTo("Nova descrição");
        assertThat(deck.getSubjectId()).isEqualTo(newSubjectId);
    }

    @Test
    void givenDeck_whenMarkingDeleted_thenIsDeletedIsTrue() {
        Deck deck = new Deck(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "CF/88");
        deck.markDeleted(Instant.parse("2026-09-17T12:00:00Z"));
        assertThat(deck.isDeleted()).isTrue();
    }
}
