package br.com.certamecards.deck.web;

import br.com.certamecards.deck.domain.Deck;
import java.time.Instant;

public record DeckResponse(
        String id,
        String subjectId,
        String name,
        String description,
        String origin,
        String originRef,
        String officialStatus,
        String originLabel,
        Instant contentUpdatedAt,
        int cardCount,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        int version,
        Long changeSeq) {

    public static DeckResponse from(Deck deck) {
        return new DeckResponse(
                deck.getId().toString(),
                deck.getSubjectId().toString(),
                deck.getName(),
                deck.getDescription(),
                deck.getOrigin().code(),
                deck.getOriginRef() == null ? null : deck.getOriginRef().toString(),
                deck.getOfficialMeta().getOfficialStatus(),
                deck.getOfficialMeta().getOriginLabel(),
                deck.getOfficialMeta().getContentUpdatedAt(),
                deck.getOfficialMeta().getCardCount(),
                deck.getAudit().getCreatedAt(),
                deck.getAudit().getUpdatedAt(),
                deck.getAudit().getDeletedAt(),
                deck.getVersion(),
                deck.getChangeSeq());
    }
}
