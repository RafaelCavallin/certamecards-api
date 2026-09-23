package br.com.certamecards.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.support.LegacySchemaFixtures;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class OfflineSyncMigrationIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    @Timeout(120)
    void givenLegacyReviewLog_whenMigratingToV7_thenCanonicalColumnsAreBackfilledDeterministically()
            throws SQLException {
        Flyway toV6 = flywayTargeting(MigrationVersion.fromVersion("6"));
        toV6.clean();
        toV6.migrate();
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Instant reviewedAt;
        try (Connection connection = POSTGRES.createConnection("")) {
            reviewedAt = insertLegacyReviewOwnedBy(connection, userId, reviewId, deviceId);
        }

        flywayTargeting(MigrationVersion.LATEST).migrate();

        try (Connection connection = POSTGRES.createConnection("");
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT event_at, event_counter, event_device_id, operation_id FROM review_logs WHERE id = ?")) {
            statement.setObject(1, reviewId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getTimestamp("event_at").toInstant()).isEqualTo(reviewedAt);
                assertThat(resultSet.getInt("event_counter")).isZero();
                assertThat(resultSet.getObject("event_device_id")).isEqualTo(deviceId);
                assertThat(resultSet.getObject("operation_id")).isEqualTo(reviewId);
            }
        }
    }

    @Test
    @Timeout(120)
    void givenMigratedDatabase_whenInsertingDuplicateOperationId_thenReviewLogsUniqueConstraintRejectsIt()
            throws SQLException {
        Flyway flyway = flywayTargeting(MigrationVersion.LATEST);
        flyway.clean();
        flyway.migrate();
        UUID userId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID sharedOperationId = UUID.randomUUID();
        try (Connection connection = POSTGRES.createConnection("")) {
            LegacySchemaFixtures.insertUser(connection, userId, "duplicado@exemplo.com", "Duplicado");
            LegacySchemaFixtures.insertSubject(connection, subjectId);
            LegacySchemaFixtures.insertDeck(connection, deckId, userId, subjectId);
            LegacySchemaFixtures.insertCard(connection, cardId, deckId);
            insertCanonicalReviewLog(connection, UUID.randomUUID(), userId, cardId, sharedOperationId);

            assertThatThrownBy(() ->
                            insertCanonicalReviewLog(connection, UUID.randomUUID(), userId, cardId, sharedOperationId))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    @Timeout(120)
    void givenMigratedDatabase_whenReservingSameOperationTwice_thenReceiptPrimaryKeyRejectsIt() throws SQLException {
        Flyway flyway = flywayTargeting(MigrationVersion.LATEST);
        flyway.clean();
        flyway.migrate();
        UUID userId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        try (Connection connection = POSTGRES.createConnection("")) {
            LegacySchemaFixtures.insertUser(connection, userId, "recibo@exemplo.com", "Recibo");
            insertReceipt(connection, userId, operationId);

            assertThatThrownBy(() -> insertReceipt(connection, userId, operationId))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    @Timeout(120)
    void givenMigratedDatabase_whenUpdatingDisplayName_thenUsersChangeSeqIsStamped() throws SQLException {
        Flyway flyway = flywayTargeting(MigrationVersion.LATEST);
        flyway.clean();
        flyway.migrate();
        UUID userId = UUID.randomUUID();
        try (Connection connection = POSTGRES.createConnection("")) {
            LegacySchemaFixtures.insertUser(connection, userId, "perfil@exemplo.com", "Antigo");

            try (PreparedStatement update =
                    connection.prepareStatement("UPDATE users SET display_name = 'Novo' WHERE id = ?")) {
                update.setObject(1, userId);
                update.execute();
            }

            try (PreparedStatement select = connection.prepareStatement("SELECT change_seq FROM users WHERE id = ?")) {
                select.setObject(1, userId);
                try (ResultSet resultSet = select.executeQuery()) {
                    resultSet.next();
                    assertThat(resultSet.getObject("change_seq")).isNotNull();
                }
            }
        }
    }

    private Flyway flywayTargeting(MigrationVersion target) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .target(target)
                .load();
    }

    private Instant insertLegacyReviewOwnedBy(Connection connection, UUID userId, UUID reviewId, UUID deviceId)
            throws SQLException {
        UUID subjectId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        LegacySchemaFixtures.insertUser(connection, userId, "candidata@exemplo.com", "Candidata");
        LegacySchemaFixtures.insertSubject(connection, subjectId);
        LegacySchemaFixtures.insertDeck(connection, deckId, userId, subjectId);
        LegacySchemaFixtures.insertCard(connection, cardId, deckId);
        Instant reviewedAt = Instant.parse("2026-09-01T12:00:00Z");
        String sql =
                """
                INSERT INTO review_logs (id, user_id, card_id, kind, reviewed_at, duration_ms, state_after, device_id)
                VALUES (?, ?, ?, 'review', ?, 4000, '{}', ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, reviewId);
            statement.setObject(2, userId);
            statement.setObject(3, cardId);
            statement.setTimestamp(4, Timestamp.from(reviewedAt));
            statement.setObject(5, deviceId);
            statement.execute();
        }
        return reviewedAt;
    }

    private void insertCanonicalReviewLog(Connection connection, UUID id, UUID userId, UUID cardId, UUID operationId)
            throws SQLException {
        String sql =
                """
                INSERT INTO review_logs
                    (id, user_id, card_id, kind, reviewed_at, duration_ms, state_after, device_id,
                     event_at, event_counter, event_device_id, operation_id)
                VALUES (?, ?, ?, 'review', now(), 4000, '{}', ?, now(), 0, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.setObject(2, userId);
            statement.setObject(3, cardId);
            statement.setObject(4, operationId);
            statement.setObject(5, operationId);
            statement.setObject(6, operationId);
            statement.execute();
        }
    }

    private void insertReceipt(Connection connection, UUID userId, UUID operationId) throws SQLException {
        String sql =
                """
                INSERT INTO sync_operation_receipts
                    (user_id, operation_id, request_hash, kind, event_at, event_counter, event_device_id, outcome)
                VALUES (?, ?, 'hash', 'deck_create', now(), 0, ?, 'applied')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, userId);
            statement.setObject(2, operationId);
            statement.setObject(3, operationId);
            statement.execute();
        }
    }
}
