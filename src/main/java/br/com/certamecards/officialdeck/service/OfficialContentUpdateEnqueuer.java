package br.com.certamecards.officialdeck.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.officialdeck.domain.ContentUpdateJob;
import br.com.certamecards.officialdeck.persistence.ContentUpdateJobStore;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OfficialContentUpdateEnqueuer {

    private final ContentUpdateJobStore jobStore;
    private final OfficialMetrics metrics;

    public OfficialContentUpdateEnqueuer(ContentUpdateJobStore jobStore, OfficialMetrics metrics) {
        this.jobStore = jobStore;
        this.metrics = metrics;
    }

    public void enqueue(Card card, String note, Instant updatedAt) {
        jobStore.enqueue(new ContentUpdateJob(null, card.getId(), card.getDeckId(), note, updatedAt, null));
        metrics.jobQueued();
    }
}
