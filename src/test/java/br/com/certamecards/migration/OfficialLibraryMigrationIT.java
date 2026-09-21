package br.com.certamecards.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.certamecards.support.LegacySchemaFixtures;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
class OfficialLibraryMigrationIT {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:18");

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @Test
    @Timeout(120)
    void givenPrd1DataWithoutUserId_whenMigratingToV4_thenReviewLogsAreBackfilledFromDeckOwner() throws SQLException {
        Flyway toV3 = flywayTargeting(MigrationVersion.fromVersion("3"));
        toV3.clean();
        toV3.migrate();
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        try (Connection connection = POSTGRES.createConnection("")) {
            insertLegacyReviewOwnedBy(connection, userId, reviewId);
        }

        flywayTargeting(MigrationVersion.LATEST).migrate();

        assertThat(backfilledUserId(reviewId)).isEqualTo(userId);
    }

    @Test
    @Timeout(120)
    void givenMigratedDatabase_whenDeletingAuditLog_thenAppendOnlyTriggerRejects() throws SQLException {
        Flyway flyway = flywayTargeting(MigrationVersion.LATEST);
        flyway.clean();
        flyway.migrate();
        UUID actorId = UUID.randomUUID();
        UUID logId = UUID.randomUUID();
        try (Connection connection = POSTGRES.createConnection("")) {
            LegacySchemaFixtures.insertUser(connection, actorId, "admin@exemplo.com", "Admin");
            LegacySchemaFixtures.insertAuditLog(connection, logId, actorId);

            assertThatThrownBy(() -> deleteAuditLog(connection, logId)).isInstanceOf(SQLException.class);
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

    private void insertLegacyReviewOwnedBy(Connection connection, UUID userId, UUID reviewId) throws SQLException {
        UUID subjectId = UUID.randomUUID();
        UUID deckId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        LegacySchemaFixtures.insertUser(connection, userId, "candidata@exemplo.com", "Candidata");
        LegacySchemaFixtures.insertSubject(connection, subjectId);
        LegacySchemaFixtures.insertDeck(connection, deckId, userId, subjectId);
        LegacySchemaFixtures.insertCard(connection, cardId, deckId);
        LegacySchemaFixtures.insertLegacyReviewLog(connection, reviewId, cardId);
    }

    private void deleteAuditLog(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM admin_audit_logs WHERE id = ?")) {
            statement.setObject(1, id);
            statement.execute();
        }
    }

    private UUID backfilledUserId(UUID reviewId) throws SQLException {
        try (Connection connection = POSTGRES.createConnection("");
                PreparedStatement statement =
                        connection.prepareStatement("SELECT user_id FROM review_logs WHERE id = ?")) {
            statement.setObject(1, reviewId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return (UUID) resultSet.getObject("user_id");
            }
        }
    }
}
