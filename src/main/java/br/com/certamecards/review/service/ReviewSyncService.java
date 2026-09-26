package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.ReviewPushResult;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewSyncService {

    private final ReviewPushContextResolver contextResolver;
    private final ReviewPushApplier applier;
    private final ReviewSyncMetricsRecorder metricsRecorder;

    public ReviewSyncService(
            ReviewPushContextResolver contextResolver,
            ReviewPushApplier applier,
            ReviewSyncMetricsRecorder metricsRecorder) {
        this.contextResolver = contextResolver;
        this.applier = applier;
        this.metricsRecorder = metricsRecorder;
    }

    @Transactional
    public ReviewPushResult push(UUID userId, ReviewPushCommand command) {
        ReviewPushContext context = contextResolver.resolve(userId, command);
        ReviewPushOutcome outcome = applier.apply(context, command);
        metricsRecorder.record(outcome.toMetricsInput(context.now()));
        return outcome.toResult();
    }
}
