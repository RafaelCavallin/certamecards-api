package br.com.certamecards.sync.domain;

public final class SyncLimits {

    public static final int DEFAULT_PAGE_LIMIT = 500;
    public static final int MIN_PAGE_LIMIT = 1;
    public static final int MAX_PAGE_LIMIT = 500;
    public static final int MAX_MUTATION_BATCH_SIZE = 100;
    public static final int MAX_MUTATION_DEPENDENCIES = 10;
    public static final int REVIEW_LOG_WINDOW_DAYS = 35;
    public static final int PURGE_WINDOW_DAYS = 30;

    private SyncLimits() {}
}
