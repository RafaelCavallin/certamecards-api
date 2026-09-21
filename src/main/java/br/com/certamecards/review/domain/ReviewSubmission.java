package br.com.certamecards.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.UUID;

@Embeddable
public class ReviewSubmission {

    @Column(nullable = false)
    private boolean offline;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected ReviewSubmission() {}

    public ReviewSubmission(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isOffline() {
        return offline;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void markReceived(Instant now) {
        this.receivedAt = now;
    }
}
