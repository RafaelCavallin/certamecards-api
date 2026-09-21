package br.com.certamecards.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TermsAcceptanceTest {

    @Test
    void givenNewInstance_whenReadingFields_thenAllAreNull() {
        TermsAcceptance terms = new TermsAcceptance();
        assertThat(terms.getAcceptedAt()).isNull();
        assertThat(terms.getVersion()).isNull();
    }

    @Test
    void givenVersionAndInstant_whenAccepting_thenFieldsAreSet() {
        TermsAcceptance terms = new TermsAcceptance();
        Instant now = Instant.parse("2026-09-17T12:00:00Z");
        terms.accept("2026-09-01", now);
        assertThat(terms.getVersion()).isEqualTo("2026-09-01");
        assertThat(terms.getAcceptedAt()).isEqualTo(now);
    }
}
