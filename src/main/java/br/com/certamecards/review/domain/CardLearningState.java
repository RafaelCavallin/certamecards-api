package br.com.certamecards.review.domain;

public enum CardLearningState {
    NEW((short) 0),
    LEARNING((short) 1),
    REVIEW((short) 2),
    RELEARNING((short) 3);

    private final short code;

    CardLearningState(short code) {
        this.code = code;
    }

    public short code() {
        return code;
    }

    public static CardLearningState fromCode(short code) {
        for (CardLearningState state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown CardLearningState code: " + code);
    }
}
