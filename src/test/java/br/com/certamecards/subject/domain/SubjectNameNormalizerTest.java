package br.com.certamecards.subject.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubjectNameNormalizerTest {

    @Test
    void givenNamesWithSpacingCaseAndAccentVariations_whenNormalizing_thenAllEqual() {
        String base = SubjectNameNormalizer.normalize(" Direito  Constitucional ");
        assertThat(SubjectNameNormalizer.normalize("direito constitucional")).isEqualTo(base);
        assertThat(SubjectNameNormalizer.normalize("Dïreito Constitucional")).isEqualTo(base);
        assertThat(base).isEqualTo("direito constitucional");
    }

    @Test
    void givenDifferentNames_whenNormalizing_thenResultsDiffer() {
        assertThat(SubjectNameNormalizer.normalize("Direito Penal"))
                .isNotEqualTo(SubjectNameNormalizer.normalize("Direito Civil"));
    }
}
