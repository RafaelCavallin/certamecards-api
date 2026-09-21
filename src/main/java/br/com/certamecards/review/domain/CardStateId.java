package br.com.certamecards.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CardStateId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "card_id")
    private UUID cardId;

    protected CardStateId() {}

    public CardStateId(UUID userId, UUID cardId) {
        this.userId = userId;
        this.cardId = cardId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCardId() {
        return cardId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CardStateId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, cardId);
    }
}
