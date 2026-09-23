package br.com.certamecards.errorreport.service;

import br.com.certamecards.errorreport.domain.ErrorReportStatus;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ErrorReportClosureMetrics {

    private static final String CLOSED_METRIC = "error_report.closed";
    private static final String OPEN_AGE_METRIC = "error_report.open_age_days";
    private static final String OUTCOME_TAG = "outcome";

    private final MeterRegistry meterRegistry;

    public ErrorReportClosureMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(ErrorReportStatus outcome, Instant createdAt, Instant closedAt) {
        meterRegistry.counter(CLOSED_METRIC, OUTCOME_TAG, outcome.code()).increment();
        meterRegistry
                .summary(OPEN_AGE_METRIC)
                .record(Duration.between(createdAt, closedAt).toDays());
    }
}
