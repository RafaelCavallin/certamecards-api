package br.com.certamecards.card.service;

import br.com.certamecards.card.domain.Card;
import br.com.certamecards.card.persistence.CardRepository;
import br.com.certamecards.common.error.ApiException;
import br.com.certamecards.common.error.ErrorCode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CardAccessResolver {

    private final CardRepository cardRepository;

    public CardAccessResolver(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public Card resolveForRead(UUID userId, UUID cardId) {
        return cardRepository
                .findAccessibleByIdAndUserId(cardId, userId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    public Card resolveForWrite(UUID userId, UUID cardId) {
        return cardRepository
                .findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> ApiException.of(ErrorCode.NOT_FOUND));
    }

    public Map<UUID, Card> resolveAllForRead(UUID userId, Collection<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        return cardRepository.findAllAccessibleByIdInAndUserId(List.copyOf(cardIds), userId).stream()
                .collect(Collectors.toMap(Card::getId, card -> card));
    }
}
