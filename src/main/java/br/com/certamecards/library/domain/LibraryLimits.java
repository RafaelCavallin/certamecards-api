package br.com.certamecards.library.domain;

public final class LibraryLimits {

    public static final int MAX_QUERY_LENGTH = 120;
    public static final int MAX_PAGE_SIZE = 50;
    public static final int MAX_CONTENT_PAGE_SIZE = 1_000;
    public static final int MAX_SUGGESTIONS = 5;
    public static final int SUGGESTION_CANDIDATE_POOL = 200;

    private LibraryLimits() {}
}
