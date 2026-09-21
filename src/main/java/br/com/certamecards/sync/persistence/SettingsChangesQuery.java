package br.com.certamecards.sync.persistence;

import br.com.certamecards.sync.domain.SettingsChange;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class SettingsChangesQuery {

    private static final String SQL =
            """
            SELECT new_per_day, reviews_per_day, focus_minutes, exam_date, time_zone, theme, change_seq
            FROM user_settings
            WHERE user_id = :userId AND change_seq > :cursor
              AND write_xid < pg_snapshot_xmin(pg_current_snapshot())
            """;

    private final JdbcClient jdbcClient;

    public SettingsChangesQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SettingsChange fetch(UUID userId, long cursor) {
        List<SettingsChange> rows = jdbcClient
                .sql(SQL)
                .param("userId", userId)
                .param("cursor", cursor)
                .query((rs, rowNum) -> new SettingsChange(
                        rs.getInt("new_per_day"),
                        rs.getInt("reviews_per_day"),
                        rs.getInt("focus_minutes"),
                        rs.getObject("exam_date", LocalDate.class),
                        rs.getString("time_zone"),
                        rs.getString("theme"),
                        rs.getLong("change_seq")))
                .list();
        return rows.isEmpty() ? null : rows.get(0);
    }
}
