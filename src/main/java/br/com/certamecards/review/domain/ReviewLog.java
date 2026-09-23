package br.com.certamecards.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "review_logs")
public class ReviewLog {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(nullable = false)
    private ReviewKind kind;

    private Short rating;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_before")
    private String stateBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_after", nullable = false)
    private String stateAfter;

    @Embedded
    private ReviewSubmission submission;

    @Column(name = "change_seq", insertable = false, updatable = false)
    private Long changeSeq;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;

    @Column(name = "event_counter", nullable = false)
    private int eventCounter;

    @Column(name = "event_device_id", nullable = false)
    private UUID eventDeviceId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    protected ReviewLog() {}

    public ReviewLog(
            UUID id, UUID userId, UUID cardId, ReviewKind kind, Instant reviewedAt, UUID deviceId, String stateAfter) {
        this.id = id;
        this.userId = userId;
        this.cardId = cardId;
        this.kind = kind;
        this.reviewedAt = reviewedAt;
        this.stateAfter = stateAfter;
        this.submission = new ReviewSubmission(deviceId);
        this.eventAt = reviewedAt;
        this.eventCounter = 0;
        this.eventDeviceId = deviceId;
        this.operationId = id;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getCardId() {
        return cardId;
    }

    public ReviewKind getKind() {
        return kind;
    }

    public Short getRating() {
        return rating;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public String getStateBefore() {
        return stateBefore;
    }

    public String getStateAfter() {
        return stateAfter;
    }

    public ReviewSubmission getSubmission() {
        return submission;
    }

    public Long getChangeSeq() {
        return changeSeq;
    }

    public void assignStateBefore(String stateBefore) {
        this.stateBefore = stateBefore;
    }
}
