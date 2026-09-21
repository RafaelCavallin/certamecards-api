package br.com.certamecards;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.certamecards.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(PostgresContainerSupport.class)
class CertameCardsApplicationIT {

    @Test
    void givenApplicationContext_whenStarted_thenContainsMainApplicationBean(ApplicationContext context) {
        assertThat(context.getBean(CertameCardsApplication.class)).isNotNull();
    }
}
