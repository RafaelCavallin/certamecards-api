package br.com.certamecards.sync.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class SubjectsChangesQueryTest {

    private final JdbcClient jdbcClient = mock(JdbcClient.class);
    private final SubjectsChangesQuery query = new SubjectsChangesQuery(jdbcClient);

    @Test
    void givenNoRemainingBudget_whenFetching_thenReturnsEmptyWithoutQuerying() {
        assertThat(query.fetch(0, 0)).isEmpty();
        verifyNoInteractions(jdbcClient);
    }
}
