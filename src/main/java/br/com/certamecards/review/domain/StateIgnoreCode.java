package br.com.certamecards.review.domain;

public enum StateIgnoreCode {
    CARD_NOT_FOUND("card_not_found"),
    CARD_DELETED("card_deleted");

    private final String code;

    StateIgnoreCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
