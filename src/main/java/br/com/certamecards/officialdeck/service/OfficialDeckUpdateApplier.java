package br.com.certamecards.officialdeck.service;

import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.subject.service.SubjectLookup;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OfficialDeckUpdateApplier {

    private final SubjectLookup subjectLookup;

    public OfficialDeckUpdateApplier(SubjectLookup subjectLookup) {
        this.subjectLookup = subjectLookup;
    }

    public Map<String, AuditChange> apply(Deck deck, UpdateOfficialDeckCommand command) {
        Map<String, AuditChange> changes = new LinkedHashMap<>();
        applySubject(deck, command.subjectId());
        applyName(deck, command.name(), changes);
        applyDescription(deck, command.description(), changes);
        return changes;
    }

    private void applySubject(Deck deck, UUID subjectId) {
        if (subjectId == null) {
            return;
        }
        subjectLookup.requireActive(subjectId);
        deck.changeSubject(subjectId);
    }

    private void applyName(Deck deck, String rawName, Map<String, AuditChange> changes) {
        if (rawName == null) {
            return;
        }
        String name = rawName.strip();
        if (!name.equals(deck.getName())) {
            changes.put("name", new AuditChange(deck.getName(), name));
            deck.rename(name);
        }
    }

    private void applyDescription(Deck deck, String rawDescription, Map<String, AuditChange> changes) {
        if (rawDescription == null) {
            return;
        }
        String description = rawDescription.isBlank() ? null : rawDescription.strip();
        if (!Objects.equals(description, deck.getDescription())) {
            changes.put("description", new AuditChange(deck.getDescription(), description));
            deck.changeDescription(description);
        }
    }
}
