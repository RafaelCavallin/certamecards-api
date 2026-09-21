package br.com.certamecards.sync.domain;

import java.util.List;

public record ChangesPage(
        List<SubjectChange> subjects,
        List<DeckChange> decks,
        List<CardChange> cards,
        List<CardStateChange> cardStates,
        List<ReviewLogChange> reviewLogs,
        List<ReviewVoidChange> reviewVoids,
        List<DeckSubscriptionChange> subscriptions,
        SettingsChange settings,
        long nextCursor,
        boolean hasMore) {}
