package br.com.certamecards.officialdeck.domain;

public enum OfficialDeckStatus {
    DRAFT("draft"),
    PUBLISHED("published"),
    DISCONTINUED("discontinued");

    private final String code;

    OfficialDeckStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static OfficialDeckStatus fromCode(String code) {
        for (OfficialDeckStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown OfficialDeckStatus code: " + code);
    }
}
