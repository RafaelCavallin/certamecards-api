package br.com.certamecards.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class TermsAcceptance {

    @Column(name = "terms_accepted_at")
    private Instant acceptedAt;

    @Column(name = "terms_version")
    private String version;

    public void accept(String version, Instant now) {
        this.version = version;
        this.acceptedAt = now;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public String getVersion() {
        return version;
    }
}
