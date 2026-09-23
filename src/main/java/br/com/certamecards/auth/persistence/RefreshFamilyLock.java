package br.com.certamecards.auth.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class RefreshFamilyLock {

    private static final String SQL = "SELECT 1 AS locked FROM pg_advisory_xact_lock(:lockKey)";

    private final JdbcClient jdbcClient;

    public RefreshFamilyLock(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void lock(UUID familyId) {
        long lockKey = familyId.getMostSignificantBits() ^ familyId.getLeastSignificantBits();
        jdbcClient.sql(SQL).param("lockKey", lockKey).query(Integer.class).single();
    }
}
