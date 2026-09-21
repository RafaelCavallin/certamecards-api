package br.com.certamecards.library.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SuggestionPickerTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:00:00Z");

    private final SuggestionPicker picker = new SuggestionPicker();
    private final UUID constitutional = UUID.randomUUID();
    private final UUID portuguese = UUID.randomUUID();
    private final UUID administrative = UUID.randomUUID();
    private final UUID informatics = UUID.randomUUID();

    @Test
    void givenTU54_whenSubjectsRepeat_thenKeepsOnlyTheFirstDeckOfEachSubject() {
        List<LibraryDeckSummary> candidates = List.of(
                deck(constitutional, "A1", false),
                deck(constitutional, "A2", false),
                deck(portuguese, "B1", false),
                deck(administrative, "C1", false));

        List<LibraryDeckSummary> picked = picker.pick(candidates, 3);

        assertThat(picked).extracting(LibraryDeckSummary::name).containsExactly("A1", "B1", "C1");
    }

    @Test
    void givenTU54_whenMoreSubjectsThanTheLimit_thenStopsAtTheLimit() {
        List<LibraryDeckSummary> candidates = List.of(
                deck(constitutional, "A1", false),
                deck(portuguese, "B1", false),
                deck(administrative, "C1", false),
                deck(informatics, "D1", false));

        assertThat(picker.pick(candidates, 3)).hasSize(3);
    }

    @Test
    void givenTU54_whenDeckIsAlreadySubscribed_thenItIsIgnored() {
        List<LibraryDeckSummary> candidates =
                List.of(deck(constitutional, "A1", true), deck(constitutional, "A2", false));

        assertThat(picker.pick(candidates, 3))
                .extracting(LibraryDeckSummary::name)
                .containsExactly("A2");
    }

    @Test
    void givenTU54_whenThereAreNoCandidates_thenReturnsEmpty() {
        assertThat(picker.pick(List.of(), 3)).isEmpty();
    }

    private LibraryDeckSummary deck(UUID subjectId, String name, boolean subscribed) {
        return new LibraryDeckSummary(UUID.randomUUID(), subjectId, "Matéria", name, null, 10, NOW, subscribed);
    }
}
