package br.com.certamecards.library.web;

import br.com.certamecards.card.web.CardResponse;
import br.com.certamecards.card.web.CardStateResponse;
import br.com.certamecards.library.service.DeckContentPage;
import java.util.List;
import java.util.UUID;

public record DeckContentResponse(
        List<CardResponse> cards, List<CardStateResponse> cardStates, UUID nextAfter, boolean hasMore) {

    public static DeckContentResponse from(DeckContentPage page) {
        return new DeckContentResponse(
                page.cards().stream().map(CardResponse::from).toList(),
                page.cardStates().stream().map(CardStateResponse::from).toList(),
                page.nextAfter(),
                page.hasMore());
    }
}
