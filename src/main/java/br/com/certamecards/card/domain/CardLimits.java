package br.com.certamecards.card.domain;

public final class CardLimits {

    public static final int MAX_FRONT_LENGTH = 1_000;
    public static final int MAX_BACK_LENGTH = 2_000;
    public static final int MAX_SOURCE_LENGTH = 120;
    public static final int MAX_CARDS_PER_DECK = 5_000;
    public static final int MAX_CARDS_PER_USER = 50_000;

    private CardLimits() {}
}
