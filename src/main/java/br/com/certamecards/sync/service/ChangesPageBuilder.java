package br.com.certamecards.sync.service;

import br.com.certamecards.sync.domain.CardChange;
import br.com.certamecards.sync.domain.CardStateChange;
import br.com.certamecards.sync.domain.ChangesPage;
import br.com.certamecards.sync.domain.DeckChange;
import br.com.certamecards.sync.domain.DeckSubscriptionChange;
import br.com.certamecards.sync.domain.ReviewLogChange;
import br.com.certamecards.sync.domain.ReviewVoidChange;
import br.com.certamecards.sync.domain.SettingsChange;
import br.com.certamecards.sync.domain.SubjectChange;
import java.util.List;
import java.util.function.ToLongFunction;

class ChangesPageBuilder {

    private final int limit;
    private int consumed;
    private long maxChangeSeq;
    private List<SubjectChange> subjects = List.of();
    private List<DeckChange> decks = List.of();
    private List<CardChange> cards = List.of();
    private List<CardStateChange> cardStates = List.of();
    private List<ReviewLogChange> reviewLogs = List.of();
    private List<ReviewVoidChange> reviewVoids = List.of();
    private List<DeckSubscriptionChange> subscriptions = List.of();
    private SettingsChange settings;

    ChangesPageBuilder(int limit) {
        this.limit = limit;
    }

    int remaining() {
        return Math.max(0, limit - consumed);
    }

    int itemCount() {
        return consumed;
    }

    void addSubjects(List<SubjectChange> items) {
        subjects = accumulate(items, SubjectChange::changeSeq);
    }

    void addDecks(List<DeckChange> items) {
        decks = accumulate(items, DeckChange::changeSeq);
    }

    void addCards(List<CardChange> items) {
        cards = accumulate(items, CardChange::changeSeq);
    }

    void addCardStates(List<CardStateChange> items) {
        cardStates = accumulate(items, CardStateChange::changeSeq);
    }

    void addReviewLogs(List<ReviewLogChange> items) {
        reviewLogs = accumulate(items, ReviewLogChange::changeSeq);
    }

    void addReviewVoids(List<ReviewVoidChange> items) {
        reviewVoids = accumulate(items, ReviewVoidChange::changeSeq);
    }

    void addSubscriptions(List<DeckSubscriptionChange> items) {
        subscriptions = accumulate(items, DeckSubscriptionChange::changeSeq);
    }

    void setSettings(SettingsChange item) {
        settings = item;
        if (item != null) {
            maxChangeSeq = Math.max(maxChangeSeq, item.changeSeq());
        }
    }

    ChangesPage build(long cursor, int limit) {
        long nextCursor = Math.max(cursor, maxChangeSeq);
        boolean hasMore = consumed >= limit;
        return new ChangesPage(
                subjects,
                decks,
                cards,
                cardStates,
                reviewLogs,
                reviewVoids,
                subscriptions,
                settings,
                nextCursor,
                hasMore);
    }

    private <T> List<T> accumulate(List<T> items, ToLongFunction<T> changeSeqOf) {
        consumed += items.size();
        items.forEach(item -> maxChangeSeq = Math.max(maxChangeSeq, changeSeqOf.applyAsLong(item)));
        return items;
    }
}
