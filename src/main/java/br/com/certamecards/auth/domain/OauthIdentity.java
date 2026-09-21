package br.com.certamecards.auth.domain;

import br.com.certamecards.common.persistence.CitextJdbcType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;

@Entity
@Table(name = "oauth_identities")
public class OauthIdentity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private OauthProvider provider;

    @Column(nullable = false)
    private String subject;

    @JdbcType(CitextJdbcType.class)
    @Column(nullable = false)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OauthIdentity() {}

    public OauthIdentity(UUID userId, OauthProvider provider, String subject, String email) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.provider = provider;
        this.subject = subject;
        this.email = email;
    }

    public OauthIdentity(UUID userId, OauthProvider provider, String subject, String email, Instant createdAt) {
        this(userId, provider, subject, email);
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public OauthProvider getProvider() {
        return provider;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
