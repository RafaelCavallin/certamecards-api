package br.com.certamecards.officialdeck.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.support.PostgresContainerSupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class OfficialSeedIT {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void givenFreshDatabase_whenMigrated_thenFourOfficialDecksWith51CardsLinkedToSubjects() {
        List<String> subjects = jdbcClient
                .sql("SELECT s.name FROM decks d JOIN subjects s ON s.id = d.subject_id "
                        + "WHERE d.owner_id IS NULL AND d.origin = 'official_subscription' ORDER BY d.name")
                .query(String.class)
                .list();
        Long cards = jdbcClient
                .sql("SELECT COUNT(*) FROM cards c JOIN decks d ON d.id = c.deck_id WHERE d.owner_id IS NULL")
                .query(Long.class)
                .single();

        assertThat(subjects)
                .containsExactlyInAnyOrder(
                        "Direito Constitucional", "Direito Administrativo", "Português", "Raciocínio Lógico");
        assertThat(cards).isEqualTo(51);
    }

    @Test
    void givenSeededDecks_whenComparingCounters_thenCardCountMatchesAndStatusIsDraft() {
        Long mismatched = jdbcClient
                .sql("SELECT COUNT(*) FROM decks d WHERE d.owner_id IS NULL AND (d.official_status <> 'draft' "
                        + "OR d.card_count <> (SELECT COUNT(*) FROM cards c WHERE c.deck_id = d.id))")
                .query(Long.class)
                .single();

        assertThat(mismatched).isZero();
    }
}
