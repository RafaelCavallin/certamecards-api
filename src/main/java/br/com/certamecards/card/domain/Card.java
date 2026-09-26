package br.com.certamecards.card.domain;

import br.com.certamecards.common.domain.ContentAudit;
import br.com.certamecards.common.sync.SyncMetadata;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    private UUID id;

    @Column(name = "deck_id", nullable = false)
    private UUID deckId;

    @Column(nullable = false)
    private CardType type;

    @Column(nullable = false)
    private String front;

    @Column(nullable = false)
    private String back;

    private String source;

    @Embedded
    private ContentAudit audit = new ContentAudit();

    @Version
    private int version;

    @Embedded
    @AttributeOverride(name = "changeSeq", column = @Column(name = "change_seq"))
    private SyncMetadata syncMetadata = new SyncMetadata();

    protected Card() {}

    public Card(UUID id, UUID deckId, String front, String back) {
        this.id = id;
        this.deckId = deckId;
        this.front = front;
        this.back = back;
        this.type = CardType.BASIC;
    }

    public void moveToDeck(UUID deckId) {
        this.deckId = deckId;
    }

    public void editContent(String front, String back, String source) {
        this.front = front;
        this.back = back;
        this.source = source;
    }

    public void touch(Instant now) {
        audit.touch(now);
    }

    public void markDeleted(Instant now) {
        audit.markDeleted(now);
    }

    public boolean isDeleted() {
        return audit.getDeletedAt() != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeckId() {
        return deckId;
    }

    public CardType getType() {
        return type;
    }

    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    public String getSource() {
        return source;
    }

    public ContentAudit getAudit() {
        return audit;
    }

    public int getVersion() {
        return version;
    }

    public Long getChangeSeq() {
        return syncMetadata.getChangeSeq();
    }
}
