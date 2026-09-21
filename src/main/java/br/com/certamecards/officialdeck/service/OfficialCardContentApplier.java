package br.com.certamecards.officialdeck.service;

import br.com.certamecards.auditlog.domain.AuditChange;
import br.com.certamecards.card.domain.Card;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OfficialCardContentApplier {

    public Map<String, AuditChange> apply(Card card, UpdateOfficialCardCommand command) {
        Map<String, AuditChange> changes = new LinkedHashMap<>();
        applyFront(card, command.front(), changes);
        applyBack(card, command.back(), changes);
        applySource(card, command.source(), changes);
        return changes;
    }

    private void applyFront(Card card, String rawFront, Map<String, AuditChange> changes) {
        if (rawFront == null) {
            return;
        }
        String front = rawFront.strip();
        if (!front.equals(card.getFront())) {
            changes.put("front", new AuditChange(card.getFront(), front));
            card.editContent(front, card.getBack(), card.getSource());
        }
    }

    private void applyBack(Card card, String rawBack, Map<String, AuditChange> changes) {
        if (rawBack == null) {
            return;
        }
        String back = rawBack.strip();
        if (!back.equals(card.getBack())) {
            changes.put("back", new AuditChange(card.getBack(), back));
            card.editContent(card.getFront(), back, card.getSource());
        }
    }

    private void applySource(Card card, String rawSource, Map<String, AuditChange> changes) {
        if (rawSource == null) {
            return;
        }
        String source = rawSource.isBlank() ? null : rawSource.strip();
        if (!Objects.equals(source, card.getSource())) {
            changes.put("source", new AuditChange(card.getSource(), source));
            card.editContent(card.getFront(), card.getBack(), source);
        }
    }
}
