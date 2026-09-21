package br.com.certamecards.testsupport.domain;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;

public enum SeedScenario {
    DUE_CARDS("due_cards"),
    LEECH_CARD("leech_card"),
    LARGE_DECK("large_deck"),
    OFFICIAL_DECK("official_deck"),
    OFFICIAL_DECK_WITH_SUBSCRIBERS("official_deck_with_subscribers");

    private final String code;

    SeedScenario(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public boolean official() {
        return this == OFFICIAL_DECK || this == OFFICIAL_DECK_WITH_SUBSCRIBERS;
    }

    public boolean withSubscribers() {
        return this == OFFICIAL_DECK_WITH_SUBSCRIBERS;
    }

    public static SeedScenario fromCode(String code) {
        for (SeedScenario scenario : values()) {
            if (scenario.code.equals(code)) {
                return scenario;
            }
        }
        throw ApiException.withDetail(ErrorCode.VALIDATION_FAILED, "Cenário de teste desconhecido.");
    }
}
