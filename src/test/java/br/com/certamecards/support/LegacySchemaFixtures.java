package br.com.certamecards.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class LegacySchemaFixtures {

    private static final UUID SYSTEM_DEVICE_ID = new UUID(0L, 0L);

    private LegacySchemaFixtures() {}

    public static void insertUser(Connection connection, UUID id, String email, String displayName)
            throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("INSERT INTO users (id, email, display_name) VALUES (?, ?, ?)")) {
            statement.setObject(1, id);
            statement.setString(2, email);
            statement.setString(3, displayName);
            statement.execute();
        }
    }

    public static void insertSubject(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO subjects (id, name, normalized_name) VALUES (?, 'Migração', 'migracao')")) {
            statement.setObject(1, id);
            statement.execute();
        }
    }

    public static void insertDeck(Connection connection, UUID id, UUID ownerId, UUID subjectId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO decks (id, owner_id, subject_id, name) VALUES (?, ?, ?, 'Deck legado')")) {
            statement.setObject(1, id);
            statement.setObject(2, ownerId);
            statement.setObject(3, subjectId);
            statement.execute();
        }
    }

    public static void insertCard(Connection connection, UUID id, UUID deckId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO cards (id, deck_id, front, back) VALUES (?, ?, 'Frente', 'Verso')")) {
            statement.setObject(1, id);
            statement.setObject(2, deckId);
            statement.execute();
        }
    }

    public static void insertLegacyReviewLog(Connection connection, UUID id, UUID cardId) throws SQLException {
        String sql =
                """
                INSERT INTO review_logs (id, card_id, kind, reviewed_at, duration_ms, state_after, device_id)
                VALUES (?, ?, 'review', now(), 4000, '{}', ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setObject(2, cardId);
            statement.setObject(3, SYSTEM_DEVICE_ID);
            statement.execute();
        }
    }

    public static void insertAuditLog(Connection connection, UUID id, UUID actorId) throws SQLException {
        String sql = "INSERT INTO admin_audit_logs (id, actor_id, action, target_type) "
                + "VALUES (?, ?, 'subject_created', 'subject')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setObject(2, actorId);
            statement.execute();
        }
    }
}
