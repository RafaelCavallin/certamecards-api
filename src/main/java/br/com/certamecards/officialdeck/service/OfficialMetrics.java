package br.com.certamecards.officialdeck.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OfficialMetrics {

    private final MeterRegistry meterRegistry;

    public OfficialMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void deckStatus(String target, String result) {
        meterRegistry
                .counter("official.deck.status", "to", target, "result", result)
                .increment();
    }

    public void jobQueued() {
        meterRegistry
                .counter("official.content_update.jobs", "result", "queued")
                .increment();
    }

    public void jobFinished(Instant requestedAt, Instant finishedAt) {
        meterRegistry
                .counter("official.content_update.jobs", "result", "finished")
                .increment();
        meterRegistry
                .summary("official.content_update.lag_seconds")
                .record(Duration.between(requestedAt, finishedAt).toSeconds());
    }

    public void applied(int subscribers) {
        meterRegistry.counter("official.content_update.applied").increment(subscribers);
    }

    public void purgedCards(int cards) {
        meterRegistry.counter("subscription.purge.cards").increment(cards);
    }
}
