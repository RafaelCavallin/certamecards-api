package br.com.certamecards.deck.service;

import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import br.com.certamecards.deck.domain.Deck;
import br.com.certamecards.deck.persistence.DeckRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OfficialDeckAccess {

    private final DeckRepository deckRepository;

    public OfficialDeckAccess(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public Deck findById(UUID deckId) {
        return deckRepository.findById(deckId).orElse(null);
    }

    public Deck findOfficial(UUID deckId) {
        return requireOfficial(deckRepository.findById(deckId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND)));
    }

    public Deck lockOfficial(UUID deckId) {
        return requireOfficial(
                deckRepository.findByIdForUpdate(deckId).orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND)));
    }

    public Deck save(Deck deck) {
        return deckRepository.save(deck);
    }

    private Deck requireOfficial(Deck deck) {
        if (deck.getOwnerId() != null) {
            throw ApiException.of(ErrorCode.NOT_FOUND);
        }
        return deck;
    }
}
