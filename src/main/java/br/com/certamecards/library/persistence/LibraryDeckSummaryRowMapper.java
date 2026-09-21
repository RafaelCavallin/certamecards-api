package br.com.certamecards.library.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.library.domain.LibraryDeckSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

final class LibraryDeckSummaryRowMapper implements RowMapper<LibraryDeckSummary> {

    static final LibraryDeckSummaryRowMapper INSTANCE = new LibraryDeckSummaryRowMapper();

    private LibraryDeckSummaryRowMapper() {}

    @Override
    public LibraryDeckSummary mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new LibraryDeckSummary(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("subject_id"),
                rs.getString("subject_name"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("card_count"),
                instant(rs, "content_updated_at"),
                rs.getBoolean("subscribed"));
    }
}
