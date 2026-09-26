package br.com.certamecards.sync.domain;

public enum ConflictReason {
    CONCURRENT_EDIT("concurrent_edit"),
    DELETE_WINS("delete_wins"),
    PARENT_DELETED("parent_deleted");

    private final String code;

    ConflictReason(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
