package br.com.certamecards.deck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class DeckOfficialMeta {

    @Column(name = "official_status")
    private String officialStatus;

    @Column(name = "origin_label")
    private String originLabel;

    @Column(name = "content_updated_at")
    private Instant contentUpdatedAt;

    @Column(name = "card_count", nullable = false)
    private int cardCount;

    @Column(name = "subscriber_count", nullable = false)
    private int subscriberCount;

    protected DeckOfficialMeta() {}

    public void markDraft(Instant now) {
        this.officialStatus = "draft";
        this.contentUpdatedAt = now;
    }

    public void changeStatus(String status) {
        this.officialStatus = status;
    }

    public void incrementCardCount() {
        this.cardCount++;
    }

    public void decrementCardCount() {
        this.cardCount--;
    }

    public void touchContentUpdatedAt(Instant now) {
        this.contentUpdatedAt = now;
    }

    public String getOfficialStatus() {
        return officialStatus;
    }

    public String getOriginLabel() {
        return originLabel;
    }

    public Instant getContentUpdatedAt() {
        return contentUpdatedAt;
    }

    public int getCardCount() {
        return cardCount;
    }

    public int getSubscriberCount() {
        return subscriberCount;
    }
}
