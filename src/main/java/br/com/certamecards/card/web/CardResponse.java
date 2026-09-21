package br.com.certamecards.card.web;

import br.com.certamecards.card.domain.Card;
import java.time.Instant;

public record CardResponse(
        String id,
        String deckId,
        String type,
        String front,
        String back,
        String source,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        int version,
        Long changeSeq) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId().toString(),
                card.getDeckId().toString(),
                card.getType().code(),
                card.getFront(),
                card.getBack(),
                card.getSource(),
                card.getAudit().getCreatedAt(),
                card.getAudit().getUpdatedAt(),
                card.getAudit().getDeletedAt(),
                card.getVersion(),
                card.getChangeSeq());
    }
}
