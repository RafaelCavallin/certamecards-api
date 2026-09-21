package br.com.certamecards.review.domain;

public enum ReviewKind {
    REVIEW("review"),
    RESET("reset"),
    CONTENT_UPDATE("content_update"),
    DUPLICATE("duplicate");

    private final String code;

    ReviewKind(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ReviewKind fromCode(String code) {
        for (ReviewKind kind : values()) {
            if (kind.code.equals(code)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown ReviewKind code: " + code);
    }
}
