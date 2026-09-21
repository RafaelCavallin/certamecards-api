package br.com.certamecards.review.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReviewSyncMetricsRecorder {

    private static final String ITEMS_METRIC = "sync.reviews.items";
    private static final String OFFLINE_RATIO_METRIC = "sync.reviews.offline_ratio";
    private static final String LAG_SECONDS_METRIC = "sync.reviews.lag_seconds";
    private static final String RESULT_TAG = "result";

    private final MeterRegistry meterRegistry;

    public ReviewSyncMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(ReviewMetricsInput input) {
        recordItemCounts(input);
        recordOfflineRatio(input.acceptedReviews());
        recordLagSeconds(input.acceptedReviews(), input.receivedAt());
    }

    private void recordItemCounts(ReviewMetricsInput input) {
        int acceptedCount = input.acceptedReviews().size()
                + input.acceptedVoids().size()
                + input.stateOutcome().applied().size();
        incrementBy("accepted", acceptedCount);
        incrementBy("rejected", input.rejectedReviews().size());
        incrementBy("stale", input.stateOutcome().stale().size());
        incrementBy("ignored", input.stateOutcome().ignored().size());
    }

    private void incrementBy(String result, int count) {
        if (count > 0) {
            meterRegistry.counter(ITEMS_METRIC, RESULT_TAG, result).increment(count);
        }
    }

    private void recordOfflineRatio(List<ReviewLogInput> acceptedReviews) {
        if (acceptedReviews.isEmpty()) {
            return;
        }
        long offlineCount =
                acceptedReviews.stream().filter(ReviewLogInput::offline).count();
        meterRegistry.summary(OFFLINE_RATIO_METRIC).record((double) offlineCount / acceptedReviews.size());
    }

    private void recordLagSeconds(List<ReviewLogInput> acceptedReviews, Instant receivedAt) {
        acceptedReviews.forEach(review -> meterRegistry
                .summary(LAG_SECONDS_METRIC)
                .record(Duration.between(review.reviewedAt(), receivedAt).getSeconds()));
    }
}
