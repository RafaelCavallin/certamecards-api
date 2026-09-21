package br.com.certamecards.review.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.review.domain.AppliedState;
import br.com.certamecards.review.domain.IgnoredState;
import br.com.certamecards.review.domain.StaleState;
import br.com.certamecards.review.domain.StateIgnoreCode;
import br.com.certamecards.review.persistence.CardStateUpsertWriter;
import br.com.certamecards.review.persistence.ReviewLogRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CardStateReconciler {

    private final ReviewLogRepository reviewLogRepository;
    private final CardStateUpsertWriter cardStateUpsertWriter;

    public CardStateReconciler(ReviewLogRepository reviewLogRepository, CardStateUpsertWriter cardStateUpsertWriter) {
        this.reviewLogRepository = reviewLogRepository;
        this.cardStateUpsertWriter = cardStateUpsertWriter;
    }

    public StateOutcome reconcile(StateReconciliationRequest request) {
        List<AppliedState> applied = new ArrayList<>();
        List<StaleState> stale = new ArrayList<>();
        List<IgnoredState> ignored = new ArrayList<>();
        for (CardStatePushInput state : request.states()) {
            StateDecision decision = reconcileOne(state, request);
            collect(decision, applied, stale, ignored);
        }
        return new StateOutcome(applied, stale, ignored);
    }

    private void collect(
            StateDecision decision, List<AppliedState> applied, List<StaleState> stale, List<IgnoredState> ignored) {
        if (decision.applied() != null) {
            applied.add(decision.applied());
        } else if (decision.stale() != null) {
            stale.add(decision.stale());
        } else {
            ignored.add(decision.ignored());
        }
    }

    private StateDecision reconcileOne(CardStatePushInput state, StateReconciliationRequest request) {
        Card card = request.accessibleCards().get(state.cardId());
        if (card == null) {
            return StateDecision.ignored(IgnoredState.of(state.cardId(), StateIgnoreCode.CARD_NOT_FOUND));
        }
        if (card.isDeleted()) {
            return StateDecision.ignored(IgnoredState.of(state.cardId(), StateIgnoreCode.CARD_DELETED));
        }
        long serverCount = reviewLogRepository.countAcceptedByCardIdAndUserId(state.cardId(), request.userId());
        if (serverCount != state.reviewCount()) {
            return StateDecision.stale(new StaleState(state.cardId(), serverCount));
        }
        long changeSeq = cardStateUpsertWriter.upsert(
                request.userId(), state.cardId(), state.snapshot(), state.reviewCount(), request.now());
        return StateDecision.applied(new AppliedState(state.cardId(), changeSeq));
    }
}
