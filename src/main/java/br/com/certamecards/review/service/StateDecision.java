package br.com.certamecards.review.service;

import br.com.certamecards.review.domain.AppliedState;
import br.com.certamecards.review.domain.IgnoredState;
import br.com.certamecards.review.domain.StaleState;

public record StateDecision(AppliedState applied, StaleState stale, IgnoredState ignored) {

    public static StateDecision applied(AppliedState applied) {
        return new StateDecision(applied, null, null);
    }

    public static StateDecision stale(StaleState stale) {
        return new StateDecision(null, stale, null);
    }

    public static StateDecision ignored(IgnoredState ignored) {
        return new StateDecision(null, null, ignored);
    }
}
