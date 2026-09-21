package br.com.certamecards.officialdeck.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.card.domain.Card;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OfficialCardContentApplierTest {

    private final OfficialCardContentApplier applier = new OfficialCardContentApplier();

    @Test
    void givenNewBack_whenApplying_thenEditsContentAndRecordsChange() {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "Pergunta", "Resposta antiga");
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand(null, "Resposta nova", null, false, null, 0);

        Map<String, AuditChange> changes = applier.apply(card, command);

        assertThat(card.getBack()).isEqualTo("Resposta nova");
        assertThat(changes).containsEntry("back", new AuditChange("Resposta antiga", "Resposta nova"));
    }

    @Test
    void givenSameFront_whenApplying_thenNoChangeRecorded() {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "Pergunta", "Resposta");
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand("Pergunta", null, null, false, null, 0);

        Map<String, AuditChange> changes = applier.apply(card, command);

        assertThat(changes).isEmpty();
    }

    @Test
    void givenBlankSource_whenApplying_thenClearsSourceAndRecordsChange() {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "Pergunta", "Resposta");
        card.editContent(card.getFront(), card.getBack(), "CF/88, art. 5º");
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand(null, null, "   ", false, null, 0);

        Map<String, AuditChange> changes = applier.apply(card, command);

        assertThat(card.getSource()).isNull();
        assertThat(changes).containsEntry("source", new AuditChange("CF/88, art. 5º", null));
    }

    @Test
    void givenNoFieldsProvided_whenApplying_thenNothingChanges() {
        Card card = new Card(UUID.randomUUID(), UUID.randomUUID(), "Pergunta", "Resposta");
        UpdateOfficialCardCommand command = new UpdateOfficialCardCommand(null, null, null, false, null, 0);

        Map<String, AuditChange> changes = applier.apply(card, command);

        assertThat(changes).isEmpty();
        assertThat(card.getFront()).isEqualTo("Pergunta");
    }
}
