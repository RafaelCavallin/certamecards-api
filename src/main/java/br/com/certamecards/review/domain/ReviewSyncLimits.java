package br.com.certamecards.review.domain;

import java.time.Duration;

public final class ReviewSyncLimits {

    public static final int MAX_BATCH_ITEMS = 200;
    public static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);
    public static final int MIN_DURATION_MS = 0;
    public static final int MAX_DURATION_MS = 600_000;
    public static final int MAX_HISTORY_RECORDS = 5000;
    public static final short MIN_RATING = 1;
    public static final short MAX_RATING = 4;

    private ReviewSyncLimits() {}
}
