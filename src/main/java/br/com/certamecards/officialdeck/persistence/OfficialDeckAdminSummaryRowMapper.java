package br.com.certamecards.officialdeck.persistence;

import static br.com.certamecards.common.persistence.ResultSetInstants.instant;

import br.com.certamecards.officialdeck.domain.OfficialDeckAdminSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

final class OfficialDeckAdminSummaryRowMapper {

    private OfficialDeckAdminSummaryRowMapper() {}

    static OfficialDeckAdminSummary map(ResultSet rs, int rowNum) throws SQLException {
        return new OfficialDeckAdminSummary(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("subject_id"),
                rs.getString("subject_name"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getInt("card_count"),
                rs.getInt("subscriber_count"),
                rs.getInt("open_report_count"),
                instant(rs, "content_updated_at"),
                rs.getInt("version"));
    }
}
