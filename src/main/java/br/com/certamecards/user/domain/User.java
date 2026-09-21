package br.com.certamecards.user.domain;

import br.com.certamecards.common.persistence.CitextJdbcType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @JdbcType(CitextJdbcType.class)
    @Column(nullable = false)
    private String email;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private UserRole role;

    @Embedded
    private TermsAcceptance terms = new TermsAcceptance();

    protected User() {}

    public User(String email, String displayName, UserRole role) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }

    public void verifyEmail(Instant now) {
        this.emailVerifiedAt = now;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void acceptTerms(String version, Instant now) {
        terms.accept(version, now);
    }

    public void promoteToAdmin() {
        this.role = UserRole.ADMIN;
    }

    public void demoteToCandidate() {
        this.role = UserRole.CANDIDATE;
    }

    @PostLoad
    private void ensureTermsInitialized() {
        if (terms == null) {
            terms = new TermsAcceptance();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public TermsAcceptance getTerms() {
        return terms;
    }
}
