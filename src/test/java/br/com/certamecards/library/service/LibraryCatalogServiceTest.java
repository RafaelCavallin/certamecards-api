package br.com.certamecards.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.certamecards.library.domain.LibraryDeckSummary;
import br.com.certamecards.library.domain.LibraryPage;
import br.com.certamecards.library.domain.LibraryPageRequest;
import br.com.certamecards.library.domain.LibraryQuery;
import br.com.certamecards.library.domain.SubjectSummary;
import br.com.certamecards.library.domain.SuggestionPicker;
import br.com.certamecards.library.persistence.LibraryCatalogQuery;
import br.com.certamecards.library.persistence.LibraryDeckLookupQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LibraryCatalogServiceTest {

    private final LibraryCatalogQuery catalogQuery = mock(LibraryCatalogQuery.class);
    private final LibraryDeckLookupQuery lookupQuery = mock(LibraryDeckLookupQuery.class);
    private final LibraryCatalogService service =
            new LibraryCatalogService(catalogQuery, lookupQuery, new SuggestionPicker());
    private final UUID userId = UUID.randomUUID();

    @Test
    void givenQuery_whenSearching_thenReturnsPageWithItemsAndTotal() {
        LibraryQuery query = new LibraryQuery(userId, "crase", null, new LibraryPageRequest(1, 20));
        List<LibraryDeckSummary> items = List.of(summary(UUID.randomUUID()));
        when(catalogQuery.search(query)).thenReturn(items);
        when(catalogQuery.count(query)).thenReturn(21L);

        LibraryPage page = service.search(query);

        assertThat(page).isEqualTo(new LibraryPage(items, 1, 20, 21));
    }

    @Test
    void whenListingSubjects_thenDelegatesToTheQuery() {
        List<SubjectSummary> subjects = List.of(new SubjectSummary(UUID.randomUUID(), "Português", 2));
        when(catalogQuery.subjectsWithContent()).thenReturn(subjects);

        assertThat(service.subjectsWithContent()).isEqualTo(subjects);
    }

    @Test
    void givenCandidatesOfTheSameSubject_whenSuggesting_thenReturnsOnePerSubject() {
        UUID subjectId = UUID.randomUUID();
        List<LibraryDeckSummary> candidates =
                List.of(summary(subjectId), summary(subjectId), summary(UUID.randomUUID()));
        when(lookupQuery.suggestionCandidates(userId, 200)).thenReturn(candidates);

        assertThat(service.suggestions(userId, 3)).containsExactly(candidates.get(0), candidates.get(2));
    }

    private LibraryDeckSummary summary(UUID subjectId) {
        return new LibraryDeckSummary(UUID.randomUUID(), subjectId, "Matéria", "Deck", null, 10, Instant.EPOCH, false);
    }
}
