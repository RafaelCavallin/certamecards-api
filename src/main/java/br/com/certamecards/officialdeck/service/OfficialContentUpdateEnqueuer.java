package br.com.certamecards.officialdeck.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OfficialContentUpdateEnqueuer {

    private final ContentUpdateJobStore jobStore;

    public OfficialContentUpdateEnqueuer(ContentUpdateJobStore jobStore) {
        this.jobStore = jobStore;
    }

    public void enqueue(Card card, String note, Instant updatedAt) {
        jobStore.enqueue(new ContentUpdateJob(null, card.getId(), card.getDeckId(), note, updatedAt, null));
    }
}
