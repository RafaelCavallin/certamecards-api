package br.com.certamecards.subject.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SubjectNameNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern EXTRA_SPACES = Pattern.compile("\\s+");

    private SubjectNameNormalizer() {}

    public static String normalize(String name) {
        String decomposed = Normalizer.normalize(name.strip(), Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("");
        String collapsedSpaces = EXTRA_SPACES.matcher(withoutDiacritics).replaceAll(" ");
        return collapsedSpaces.toLowerCase(Locale.ROOT);
    }
}
