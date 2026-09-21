package br.com.certamecards.card.domain;

public enum CardType {
    BASIC("basic"),
    CLOZE("cloze"),
    TRUE_FALSE("true_false");

    private final String code;

    CardType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static CardType fromCode(String code) {
        for (CardType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown CardType code: " + code);
    }
}
