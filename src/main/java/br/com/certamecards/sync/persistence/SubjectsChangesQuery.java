package br.com.certamecards.sync.persistence;

import br.com.certamecards.sync.domain.SubjectChange;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SubjectsChangesQuery {

    private static final String SQL =
            """
            SELECT id, name, active, change_seq FROM subjects
            WHERE change_seq > :cursor AND write_xid < pg_snapshot_xmin(pg_current_snapshot())
            ORDER BY change_seq LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public SubjectsChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<SubjectChange> fetch(long cursor, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbcClient
                .sql(SQL)
                .param("cursor", cursor)
                .param("limit", limit)
                .query((rs, rowNum) -> new SubjectChange(
                        (UUID) rs.getObject("id"),
                        rs.getString("name"),
                        rs.getBoolean("active"),
                        rs.getLong("change_seq")))
                .list();
    }
}
