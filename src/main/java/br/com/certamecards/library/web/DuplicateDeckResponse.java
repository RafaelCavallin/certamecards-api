package br.com.certamecards.library.web;

import br.com.certamecards.deck.web.DeckResponse;
import br.com.certamecards.library.service.DuplicationResult;

public record DuplicateDeckResponse(DeckResponse deck, int copiedCards, int carriedStates, long cursorHint) {

    public static DuplicateDeckResponse from(DuplicationResult result) {
        return new DuplicateDeckResponse(
                DeckResponse.from(result.deck()),
                result.counts().copiedCards(),
                result.counts().carriedStates(),
                result.counts().cursorHint());
    }
}
