package br.com.certamecards.deck.domain;

public enum DeckOrigin {
    OWN("own"),
    OFFICIAL_SUBSCRIPTION("official_subscription"),
    OFFICIAL_COPY("official_copy"),
    COMMUNITY_COPY("community_copy");

    private final String code;

    DeckOrigin(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static DeckOrigin fromCode(String code) {
        for (DeckOrigin origin : values()) {
            if (origin.code.equals(code)) {
                return origin;
            }
        }
        throw new IllegalArgumentException("Unknown DeckOrigin code: " + code);
    }
}
