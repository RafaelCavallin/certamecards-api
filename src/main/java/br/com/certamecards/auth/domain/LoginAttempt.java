package br.com.certamecards.auth.domain;

import br.com.certamecards.common.persistence.CitextJdbcType;
import br.com.certamecards.common.persistence.InetJdbcType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    private UUID id;

    @JdbcType(CitextJdbcType.class)
    @Column(nullable = false)
    private String email;

    @JdbcType(InetJdbcType.class)
    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected LoginAttempt() {}

    public LoginAttempt(String email, String ip, boolean success) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.ip = ip;
        this.success = success;
    }

    public LoginAttempt(String email, String ip, boolean success, Instant attemptedAt) {
        this(email, ip, success);
        this.attemptedAt = attemptedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getIp() {
        return ip;
    }

    public boolean isSuccess() {
        return success;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }
}
