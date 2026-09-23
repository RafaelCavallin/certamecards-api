package br.com.certamecards.sync.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SyncOperationKind {
    DECK_CREATE("deck_create"),
    DECK_UPDATE("deck_update"),
    DECK_DELETE("deck_delete"),
    CARD_CREATE("card_create"),
    CARD_UPDATE("card_update"),
    CARD_DELETE("card_delete"),
    CARD_SUSPENSION("card_suspension"),
    DECK_RESET("deck_reset"),
    SETTINGS_PATCH("settings_patch"),
    PROFILE_PATCH("profile_patch"),
    CONFLICT_RESTORE("conflict_restore");

    private final String value;

    SyncOperationKind(String value) {
        this.value = value;
    }

    @JsonCreator
    public static SyncOperationKind from(String value) {
        for (SyncOperationKind kind : values()) if (kind.value.equals(value)) return kind;
        throw new IllegalArgumentException("Unsupported sync operation kind");
    }

    @JsonValue
    public String value() {
        return value;
    }
}
