package br.com.certamecards.common.error;

public class CardLimitException extends ApiException {

    private static final String DETAIL_TEMPLATE =
            "Este deck tem %d cartões e você só tem espaço para %d. Exclua cartões ou cancele uma inscrição.";

    private final int requiredCards;
    private final int availableCards;

    public CardLimitException(int requiredCards, int availableCards) {
        super(ErrorCode.USER_CARD_LIMIT, DETAIL_TEMPLATE.formatted(requiredCards, availableCards), null, null);
        this.requiredCards = requiredCards;
        this.availableCards = availableCards;
    }

    public int getRequiredCards() {
        return requiredCards;
    }

    public int getAvailableCards() {
        return availableCards;
    }
}
