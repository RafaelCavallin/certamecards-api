package br.com.certamecards.sync.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class GlobalChangesQueryTest {

    private final JdbcClient jdbcClient = mock(JdbcClient.class);
    private final GlobalChangesQuery query = new GlobalChangesQuery(jdbcClient);

    @Test
    void givenNoRemainingBudget_whenFetching_thenReturnsEmptyWithoutQuerying() {
        assertThat(query.fetch(UUID.randomUUID(), 0, 0, Instant.now())).isEmpty();
        verifyNoInteractions(jdbcClient);
    }
}
