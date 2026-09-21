package br.com.certamecards.officialdeck.service;

import br.com.certamecards.subject.domain.SubjectNameNormalizer;

final class OfficialSearchText {

    private OfficialSearchText() {}

    static String build(String name, String description) {
        String combined = description == null ? name : name + " " + description;
        return SubjectNameNormalizer.normalize(combined);
    }
}
