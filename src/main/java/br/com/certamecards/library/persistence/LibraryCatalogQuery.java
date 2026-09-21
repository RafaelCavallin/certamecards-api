package br.com.certamecards.library.persistence;

import br.com.certamecards.library.domain.LibraryDeckSummary;
import br.com.certamecards.library.domain.LibraryQuery;
import br.com.certamecards.library.domain.LibrarySearchTextNormalizer;
import br.com.certamecards.library.domain.SubjectSummary;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LibraryCatalogQuery {

    private static final String SEARCH_FILTER =
            "AND d.search_text LIKE :pattern ESCAPE '\\' AND (CAST(:subjectId AS uuid) IS NULL OR d.subject_id = :subjectId) ";
    private static final String SEARCH_ORDER =
            "ORDER BY s.normalized_name, lower(d.name), d.id LIMIT :limit OFFSET :offset";
    private static final String COUNT_SQL =
            "SELECT count(*) FROM decks d " + LibrarySql.PUBLISHED_FILTER + SEARCH_FILTER;
    private static final String SUBJECTS_SQL =
            """
            SELECT s.id, s.name, count(*) AS deck_count
            FROM decks d JOIN subjects s ON s.id = d.subject_id
            WHERE d.official_status = 'published' AND d.deleted_at IS NULL
            GROUP BY s.id, s.name, s.normalized_name
            ORDER BY s.normalized_name
            """;

    private final JdbcClient jdbcClient;

    public LibraryCatalogQuery(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<LibraryDeckSummary> search(LibraryQuery query) {
        String sql = LibrarySql.SUMMARY_SELECT + LibrarySql.PUBLISHED_FILTER + SEARCH_FILTER + SEARCH_ORDER;
        return jdbcClient
                .sql(sql)
                .param("userId", query.userId())
                .param("pattern", LibrarySearchTextNormalizer.likePattern(query.text()))
                .param("subjectId", query.subjectId(), Types.OTHER)
                .param("limit", query.pageRequest().size())
                .param("offset", query.pageRequest().offset())
                .query(LibraryDeckSummaryRowMapper.INSTANCE)
                .list();
    }

    public long count(LibraryQuery query) {
        return jdbcClient
                .sql(COUNT_SQL)
                .param("pattern", LibrarySearchTextNormalizer.likePattern(query.text()))
                .param("subjectId", query.subjectId(), Types.OTHER)
                .query(Long.class)
                .single();
    }

    public List<SubjectSummary> subjectsWithContent() {
        return jdbcClient
                .sql(SUBJECTS_SQL)
                .query((rs, rowNum) ->
                        new SubjectSummary((UUID) rs.getObject("id"), rs.getString("name"), rs.getInt("deck_count")))
                .list();
    }
}
