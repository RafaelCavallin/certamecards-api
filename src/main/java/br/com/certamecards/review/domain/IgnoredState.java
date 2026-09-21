package br.com.certamecards.review.domain;

import java.util.UUID;

public record IgnoredState(UUID cardId, String reason) {

    public static IgnoredState of(UUID cardId, StateIgnoreCode reason) {
        return new IgnoredState(cardId, reason.code());
    }
}
