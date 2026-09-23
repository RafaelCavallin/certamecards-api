package br.com.certamecards.sync.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MutationOutcome {
    APPLIED,
    DUPLICATE,
    CONFLICT,
    ACTION_REQUIRED,
    DEPENDENCY_BLOCKED;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }
}
