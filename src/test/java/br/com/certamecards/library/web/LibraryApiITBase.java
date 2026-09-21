package br.com.certamecards.library.web;

import br.com.certamecards.subject.persistence.SubjectRepository;
import br.com.certamecards.support.MutableClock;
import br.com.certamecards.support.PostgresContainerSupport;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresContainerSupport.class, LibraryApiITBase.MutableClockConfig.class})
abstract class LibraryApiITBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcClient jdbcClient;

    @Autowired
    protected MutableClock mutableClock;

    @Autowired
    protected SubjectRepository subjectRepository;

    @MockitoSpyBean
    protected JavaMailSender mailSender;

    protected LibraryTestAccounts accounts;
    protected LibraryTestCatalog catalog;
    protected LibraryTestFixtures fixtures;
    protected LibraryTestAdminCards adminCards;
    protected LibraryTestProgress progress;

    @BeforeEach
    void createHelpers() {
        LibraryTestBeans beans =
                new LibraryTestBeans(mockMvc, objectMapper, jdbcClient, mailSender, mutableClock, subjectRepository);
        accounts = new LibraryTestAccounts(beans);
        catalog = new LibraryTestCatalog(beans);
        fixtures = new LibraryTestFixtures(beans);
        adminCards = new LibraryTestAdminCards(beans);
        progress = new LibraryTestProgress(beans);
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@exemplo.com";
    }

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.now(), ZoneOffset.UTC);
        }
    }
}
