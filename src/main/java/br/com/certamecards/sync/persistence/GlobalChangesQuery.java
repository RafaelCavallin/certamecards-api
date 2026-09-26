package br.com.certamecards.sync.persistence;

import br.com.certamecards.sync.domain.ChangeEntry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.util.RawValue;

@Component
public class GlobalChangesQuery {

    private final JdbcClient jdbcClient;

    public GlobalChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<ChangeEntry> fetch(UUID userId, long cursor, int limit, Instant reviewLogWindowStart) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(GlobalChangesSql.QUERY)
                .param("userId", userId)
                .param("cursor", cursor)
                .param("windowStart", Timestamp.from(reviewLogWindowStart))
                .param("limit", limit)
                .query((rs, rowNum) -> new ChangeEntry(
                        rs.getLong("change_seq"), rs.getString("type"), new RawValue(rs.getString("payload"))))
                .list();
    }
}
