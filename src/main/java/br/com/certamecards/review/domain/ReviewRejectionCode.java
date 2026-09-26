package br.com.certamecards.review.domain;

public enum ReviewRejectionCode {
    UNKNOWN_CARD("unknown_card"),
    INVALID_REVIEW("invalid_review");

    private final String code;

    ReviewRejectionCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
