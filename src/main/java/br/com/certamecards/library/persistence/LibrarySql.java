package br.com.certamecards.library.persistence;

final class LibrarySql {

    static final String SUMMARY_SELECT =
            """
            SELECT d.id, d.subject_id, s.name AS subject_name, d.name, d.description, d.card_count,
                   d.content_updated_at,
                   EXISTS (SELECT 1 FROM deck_subscriptions sub
                           WHERE sub.deck_id = d.id AND sub.user_id = :userId
                             AND sub.cancelled_at IS NULL) AS subscribed
            FROM decks d JOIN subjects s ON s.id = d.subject_id
            """;

    static final String PUBLISHED_FILTER = "WHERE d.official_status = 'published' AND d.deleted_at IS NULL ";

    private LibrarySql() {}
}
