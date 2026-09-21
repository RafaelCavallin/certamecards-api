package br.com.certamecards.testsupport.web;

import br.com.certamecards.testsupport.service.SeedResult;
import java.util.List;

public record SeedResponse(String deckId, List<String> cardIds) {

    public static SeedResponse from(SeedResult result) {
        return new SeedResponse(
                result.deckId().toString(),
                result.cardIds().stream().map(Object::toString).toList());
    }
}
