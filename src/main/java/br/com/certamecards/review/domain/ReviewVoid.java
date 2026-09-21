package br.com.certamecards.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_voids")
public class ReviewVoid {

    @Id
    @Column(name = "review_id")
    private UUID reviewId;

    @Column(name = "voided_at", nullable = false)
    private Instant voidedAt;

    @Column(name = "change_seq", insertable = false, updatable = false)
    private Long changeSeq;

    protected ReviewVoid() {}

    public ReviewVoid(UUID reviewId, Instant voidedAt) {
        this.reviewId = reviewId;
        this.voidedAt = voidedAt;
    }

    public UUID getReviewId() {
        return reviewId;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public Long getChangeSeq() {
        return changeSeq;
    }
}
