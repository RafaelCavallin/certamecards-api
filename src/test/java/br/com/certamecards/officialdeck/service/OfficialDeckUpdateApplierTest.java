package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.subject.service.SubjectLookup;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialDeckUpdateApplierTest {

    private final SubjectLookup subjectLookup = mock(SubjectLookup.class);
    private final OfficialDeckUpdateApplier applier = new OfficialDeckUpdateApplier(subjectLookup);

    @Test
    void givenNewName_whenApplying_thenRenamesAndRecordsChange() {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "Antigo nome", officialOrigin());
        UpdateOfficialDeckCommand command = new UpdateOfficialDeckCommand(null, "Novo nome", null, 0);

        Map<String, AuditChange> changes = applier.apply(deck, command);

        assertThat(deck.getName()).isEqualTo("Novo nome");
        assertThat(changes).containsEntry("name", new AuditChange("Antigo nome", "Novo nome"));
    }

    @Test
    void givenSameName_whenApplying_thenNoChangeRecorded() {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "Mesmo nome", officialOrigin());
        UpdateOfficialDeckCommand command = new UpdateOfficialDeckCommand(null, "Mesmo nome", null, 0);

        Map<String, AuditChange> changes = applier.apply(deck, command);

        assertThat(changes).isEmpty();
    }

    @Test
    void givenBlankDescription_whenApplying_thenClearsDescriptionAndRecordsChange() {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "Deck", officialOrigin());
        deck.changeDescription("Descrição antiga");
        UpdateOfficialDeckCommand command = new UpdateOfficialDeckCommand(null, null, "   ", 0);

        Map<String, AuditChange> changes = applier.apply(deck, command);

        assertThat(deck.getDescription()).isNull();
        assertThat(changes).containsEntry("description", new AuditChange("Descrição antiga", null));
    }

    @Test
    void givenNewSubjectId_whenApplying_thenValidatesAndChangesSubject() {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "Deck", officialOrigin());
        UUID newSubjectId = UUID.randomUUID();
        UpdateOfficialDeckCommand command = new UpdateOfficialDeckCommand(newSubjectId, null, null, 0);

        applier.apply(deck, command);

        assertThat(deck.getSubjectId()).isEqualTo(newSubjectId);
        verify(subjectLookup).requireActive(newSubjectId);
    }

    @Test
    void givenNoFieldsToChange_whenApplying_thenSubjectLookupIsNeverCalled() {
        Deck deck = new Deck(UUID.randomUUID(), null, UUID.randomUUID(), "Deck", officialOrigin());
        UpdateOfficialDeckCommand command = new UpdateOfficialDeckCommand(null, null, null, 0);

        applier.apply(deck, command);

        verifyNoInteractions(subjectLookup);
    }

    private br.com.certamecards.deck.domain.DeckOrigin officialOrigin() {
        return br.com.certamecards.deck.domain.DeckOrigin.OFFICIAL_SUBSCRIPTION;
    }
}
