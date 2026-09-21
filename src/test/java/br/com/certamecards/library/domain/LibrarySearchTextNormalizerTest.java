package br.com.certamecards.library.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LibrarySearchTextNormalizerTest {

    @Test
    void givenTU49_whenBuildingDeckText_thenJoinsNameAndDescriptionWithoutAccentsOrCase() {
        String text = LibrarySearchTextNormalizer.forDeck("Crase,  Regência", "Casos  Obrigatórios");

        assertThat(text).isEqualTo("crase, regencia casos obrigatorios");
    }

    @Test
    void givenTU49_whenDescriptionIsMissing_thenUsesOnlyTheName() {
        assertThat(LibrarySearchTextNormalizer.forDeck("  CF/88 ", null)).isEqualTo("cf/88");
    }

    @Test
    void givenTU49_whenQueryIsNull_thenItBecomesEmpty() {
        assertThat(LibrarySearchTextNormalizer.forQuery(null)).isEmpty();
    }

    @Test
    void givenTU49_whenQueryHasAccentsAndSpaces_thenItIsNormalized() {
        assertThat(LibrarySearchTextNormalizer.forQuery("  ÁGUA   Ação ")).isEqualTo("agua acao");
    }

    @Test
    void givenTU49_whenQueryHasLikeWildcards_thenTheyAreEscaped() {
        assertThat(LibrarySearchTextNormalizer.likePattern("100%_a\\b")).isEqualTo("%100\\%\\_a\\\\b%");
    }

    @Test
    void givenTU49_whenQueryIsEmpty_thenPatternMatchesEverything() {
        assertThat(LibrarySearchTextNormalizer.likePattern("")).isEqualTo("%%");
    }
}
