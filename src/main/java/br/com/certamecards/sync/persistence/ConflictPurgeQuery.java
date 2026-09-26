package br.com.certamecards.sync.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class ConflictPurgeQuery {

    private static final String EXPIRE_SQL =
            """
            UPDATE sync_conflicts SET losing_snapshot = NULL, winning_snapshot = NULL, expired_at = :now
            WHERE expires_at <= :now AND expired_at IS NULL
            """;

    private final JdbcClient jdbcClient;

    public ConflictPurgeQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public int expireDueConflicts(Instant now) {
        return jdbcClient.sql(EXPIRE_SQL).param("now", Timestamp.from(now)).update();
    }
}
