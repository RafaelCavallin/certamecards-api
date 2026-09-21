package br.com.certamecards.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.certamecards.support.PostgresContainerSupport;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class MigrationsIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void givenMigratedDatabase_whenValidating_thenSchemaHistoryIsClean() {
        assertThatCode(() -> flyway.validate()).doesNotThrowAnyException();
    }

    @Test
    void givenMigratedDatabase_whenCountingSubjects_thenSevenSeededSubjectsExist() {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM subjects", Integer.class);
        assertThat(count).isEqualTo(7);
    }
}
