package br.com.certamecards.events.domain;

public enum ProductEventName {
    SIGNUP_COMPLETED("signup_completed"),
    DECK_CREATED("deck_created"),
    CARD_CREATED("card_created"),
    SESSION_STARTED("session_started"),
    SESSION_ENDED("session_ended"),
    REVIEW_UNDONE("review_undone"),
    PWA_INSTALLED("pwa_installed"),
    SYNC_FLUSHED("sync_flushed"),
    CLIENT_ERROR("client_error");

    private final String code;

    ProductEventName(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ProductEventName fromCode(String code) {
        for (ProductEventName name : values()) {
            if (name.code.equals(code)) {
                return name;
            }
        }
        throw new IllegalArgumentException("Unknown ProductEventName code: " + code);
    }
}
