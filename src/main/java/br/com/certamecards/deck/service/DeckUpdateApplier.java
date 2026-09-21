package br.com.certamecards.deck.service;

import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.subject.service.SubjectLookup;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DeckUpdateApplier {

    private final SubjectLookup subjectLookup;

    public DeckUpdateApplier(SubjectLookup subjectLookup) {
        this.subjectLookup = subjectLookup;
    }

    public void apply(Deck deck, UpdateDeckCommand command) {
        applySubject(deck, command.subjectId());
        applyContent(deck, command.content());
    }

    private void applySubject(Deck deck, UUID subjectId) {
        if (subjectId == null) {
            return;
        }
        subjectLookup.requireActive(subjectId);
        deck.changeSubject(subjectId);
    }

    private void applyContent(Deck deck, DeckContent content) {
        if (content == null) {
            return;
        }
        if (content.name() != null) {
            deck.rename(content.name().strip());
        }
        if (content.description() != null) {
            deck.changeDescription(content.normalizedDescription());
        }
    }
}
