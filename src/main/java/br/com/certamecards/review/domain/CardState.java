package br.com.certamecards.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "card_states")
public class CardState {

    @EmbeddedId
    private CardStateId id;

    @Embedded
    private FsrsProgress progress;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(nullable = false)
    private boolean suspended;

    @Column(name = "content_update_note")
    private String contentUpdateNote;

    @Column(name = "content_updated_at")
    private Instant contentUpdatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "change_seq", insertable = false, updatable = false)
    private Long changeSeq;

    protected CardState() {}

    public CardState(CardStateId id, Instant due) {
        this.id = id;
        this.progress = new FsrsProgress(due);
    }

    public CardStateId getId() {
        return id;
    }

    public FsrsProgress getProgress() {
        return progress;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String getContentUpdateNote() {
        return contentUpdateNote;
    }

    public Instant getContentUpdatedAt() {
        return contentUpdatedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getChangeSeq() {
        return changeSeq;
    }

    public void setSuspended(boolean suspended, Instant now) {
        this.suspended = suspended;
        this.updatedAt = now;
    }

    public void reset(Instant now) {
        progress.resetToNew(now);
        this.reviewCount++;
        this.updatedAt = now;
    }

    public void seedProgress(FsrsProgressSeed seed, int reviewCount, Instant now) {
        progress.applySeed(seed);
        this.reviewCount = reviewCount;
        this.updatedAt = now;
    }

    public void initializeReviewCount(int reviewCount, Instant now) {
        this.reviewCount = reviewCount;
        this.updatedAt = now;
    }
}
