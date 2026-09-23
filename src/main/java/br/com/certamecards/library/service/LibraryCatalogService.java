package br.com.certamecards.library.service;

import br.com.certamecards.library.domain.LibraryDeckSummary;
import br.com.certamecards.library.domain.LibraryLimits;
import br.com.certamecards.library.domain.LibraryPage;
import br.com.certamecards.library.domain.LibraryQuery;
import br.com.certamecards.library.domain.SubjectSummary;
import br.com.certamecards.library.domain.SuggestionPicker;
import br.com.certamecards.library.persistence.LibraryCatalogQuery;
import br.com.certamecards.library.persistence.LibraryDeckLookupQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LibraryCatalogService {

    private final LibraryCatalogQuery catalogQuery;
    private final LibraryDeckLookupQuery lookupQuery;
    private final SuggestionPicker suggestionPicker;
    private final LibraryMetrics metrics;

    public LibraryCatalogService(
            LibraryCatalogQuery catalogQuery,
            LibraryDeckLookupQuery lookupQuery,
            SuggestionPicker suggestionPicker,
            LibraryMetrics metrics) {
        this.catalogQuery = catalogQuery;
        this.lookupQuery = lookupQuery;
        this.suggestionPicker = suggestionPicker;
        this.metrics = metrics;
    }

    public LibraryPage search(LibraryQuery query) {
        List<LibraryDeckSummary> items = catalogQuery.search(query);
        long total = catalogQuery.count(query);
        boolean filtered = query.text() != null && !query.text().isBlank() || query.subjectId() != null;
        metrics.search(filtered, total == 0);
        return new LibraryPage(
                items, query.pageRequest().page(), query.pageRequest().size(), total);
    }

    public List<SubjectSummary> subjectsWithContent() {
        return catalogQuery.subjectsWithContent();
    }

    public List<LibraryDeckSummary> suggestions(UUID userId, int limit) {
        List<LibraryDeckSummary> candidates =
                lookupQuery.suggestionCandidates(userId, LibraryLimits.SUGGESTION_CANDIDATE_POOL);
        return suggestionPicker.pick(candidates, limit);
    }
}
