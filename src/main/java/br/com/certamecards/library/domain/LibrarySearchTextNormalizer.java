package br.com.certamecards.library.domain;

import br.com.certamecards.subject.domain.SubjectNameNormalizer;

public final class LibrarySearchTextNormalizer {

    private static final String LIKE_ESCAPE = "\\";
    private static final String LIKE_ANY_SEQUENCE = "%";
    private static final String LIKE_ANY_CHARACTER = "_";

    private LibrarySearchTextNormalizer() {}

    public static String forDeck(String name, String description) {
        String combined = description == null ? name : name + " " + description;
        return SubjectNameNormalizer.normalize(combined);
    }

    public static String forQuery(String query) {
        return query == null ? "" : SubjectNameNormalizer.normalize(query);
    }

    public static String likePattern(String query) {
        String escaped = forQuery(query)
                .replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
                .replace(LIKE_ANY_SEQUENCE, LIKE_ESCAPE + LIKE_ANY_SEQUENCE)
                .replace(LIKE_ANY_CHARACTER, LIKE_ESCAPE + LIKE_ANY_CHARACTER);
        return LIKE_ANY_SEQUENCE + escaped + LIKE_ANY_SEQUENCE;
    }
}
